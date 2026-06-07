package com.nezhahq.agent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class DohResolver {
    private static final int TYPE_A = 1;
    private static final int TYPE_TXT = 16;
    private static final int TYPE_AAAA = 28;
    private static final int CLASS_IN = 1;
    private static final int CLASS_CH = 3;
    private static final List<WhoamiQuery> PUBLIC_IP_QUERIES = List.of(
            new WhoamiQuery("whoami.cloudflare", CLASS_CH),
            new WhoamiQuery("o-o.myaddr.l.google.com", CLASS_IN));
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private DohResolver() {
    }

    public static Optional<InetAddress> resolveFirst(String host, String endpoints) {
        return resolveFirst(host, parseEndpoints(endpoints));
    }

    public static Optional<InetAddress> resolveFirst(String host, List<String> endpoints) {
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        Optional<InetAddress> literal = literalAddress(host.trim());
        if (literal.isPresent()) {
            return literal;
        }
        if (endpoints == null || endpoints.isEmpty()) {
            return Optional.empty();
        }
        for (String endpoint : endpoints) {
            Optional<InetAddress> address = queryEndpoint(endpoint, host, TYPE_A);
            if (address.isPresent()) {
                return address;
            }
            address = queryEndpoint(endpoint, host, TYPE_AAAA);
            if (address.isPresent()) {
                return address;
            }
        }
        return Optional.empty();
    }

    public static Optional<String> resolvePublicIpv4(String endpoints) {
        return resolvePublicIpv4(parseEndpoints(endpoints));
    }

    public static Optional<String> resolvePublicIpv4(List<String> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) {
            return Optional.empty();
        }
        for (String endpoint : endpoints) {
            for (WhoamiQuery query : PUBLIC_IP_QUERIES) {
                Optional<String> ip = queryEndpointTxt(endpoint, query.host(), query.dnsClass()).stream()
                        .map(String::trim)
                        .map(text -> text.replace("\"", ""))
                        .filter(DohResolver::isIpv4Literal)
                        .findFirst();
                if (ip.isPresent()) {
                    return ip;
                }
            }
        }
        return Optional.empty();
    }

    public static List<String> parseEndpoints(String value) {
        List<String> endpoints = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return endpoints;
        }
        for (String endpoint : value.split(",")) {
            String trimmed = endpoint.trim();
            if (!trimmed.isEmpty()) {
                endpoints.add(trimmed);
            }
        }
        return endpoints;
    }

    public static boolean isIpLiteral(String host) {
        return literalAddress(host).isPresent();
    }

    private static Optional<InetAddress> literalAddress(String host) {
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        String text = host.trim();
        try {
            if (isIpv4Literal(text) || text.indexOf(':') >= 0) {
                return Optional.of(InetAddress.getByName(text));
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private static boolean isIpv4Literal(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }
            for (int i = 0; i < part.length(); i++) {
                if (!Character.isDigit(part.charAt(i))) {
                    return false;
                }
            }
            int number = Integer.parseInt(part);
            if (number < 0 || number > 255) {
                return false;
            }
        }
        return true;
    }

    private static Optional<InetAddress> queryEndpoint(String endpoint, String host, int type) {
        try {
            HttpResponse<byte[]> response = sendDnsQuery(endpoint, buildQuery(host, type, CLASS_IN));
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return parseResponse(response.body(), type);
        } catch (IOException error) {
            return Optional.empty();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (RuntimeException error) {
            return Optional.empty();
        }
    }

    private static List<String> queryEndpointTxt(String endpoint, String host, int dnsClass) {
        try {
            HttpResponse<byte[]> response = sendDnsQuery(endpoint, buildQuery(host, TYPE_TXT, dnsClass));
            if (response.statusCode() != 200) {
                return List.of();
            }
            return parseTxtResponse(response.body());
        } catch (IOException error) {
            return List.of();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (RuntimeException error) {
            return List.of();
        }
    }

    private static HttpResponse<byte[]> sendDnsQuery(String endpoint, byte[] query) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/dns-message")
                .header("Content-Type", "application/dns-message")
                .POST(HttpRequest.BodyPublishers.ofByteArray(query))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static byte[] buildQuery(String host, int type, int dnsClass) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeUint16(out, 0x4e5a);
        writeUint16(out, 0x0100);
        writeUint16(out, 1);
        writeUint16(out, 0);
        writeUint16(out, 0);
        writeUint16(out, 0);
        writeName(out, host);
        writeUint16(out, type);
        writeUint16(out, dnsClass);
        return out.toByteArray();
    }

    private static void writeName(ByteArrayOutputStream out, String host) throws IOException {
        String ascii = IDN.toASCII(host.endsWith(".") ? host.substring(0, host.length() - 1) : host);
        for (String label : ascii.split("\\.")) {
            byte[] bytes = label.getBytes(StandardCharsets.US_ASCII);
            if (bytes.length == 0 || bytes.length > 63) {
                throw new IOException("invalid DNS label: " + label);
            }
            out.write(bytes.length);
            out.write(bytes);
        }
        out.write(0);
    }

    private static Optional<InetAddress> parseResponse(byte[] response, int expectedType) throws IOException {
        if (response == null || response.length < 12) {
            return Optional.empty();
        }
        int questionCount = readUint16(response, 4);
        int answerCount = readUint16(response, 6);
        int offset = 12;
        for (int i = 0; i < questionCount; i++) {
            offset = skipName(response, offset);
            if (offset + 4 > response.length) {
                return Optional.empty();
            }
            offset += 4;
        }
        for (int i = 0; i < answerCount; i++) {
            offset = skipName(response, offset);
            if (offset + 10 > response.length) {
                return Optional.empty();
            }
            int type = readUint16(response, offset);
            int dnsClass = readUint16(response, offset + 2);
            int dataLength = readUint16(response, offset + 8);
            offset += 10;
            if (offset + dataLength > response.length) {
                return Optional.empty();
            }
            if (dnsClass == CLASS_IN && type == expectedType && (dataLength == 4 || dataLength == 16)) {
                return Optional.of(InetAddress.getByAddress(Arrays.copyOfRange(response, offset, offset + dataLength)));
            }
            offset += dataLength;
        }
        return Optional.empty();
    }

    private static List<String> parseTxtResponse(byte[] response) throws IOException {
        if (response == null || response.length < 12) {
            return List.of();
        }
        int questionCount = readUint16(response, 4);
        int answerCount = readUint16(response, 6);
        int offset = 12;
        for (int i = 0; i < questionCount; i++) {
            offset = skipName(response, offset);
            if (offset + 4 > response.length) {
                return List.of();
            }
            offset += 4;
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < answerCount; i++) {
            offset = skipName(response, offset);
            if (offset + 10 > response.length) {
                return values;
            }
            int type = readUint16(response, offset);
            int dataLength = readUint16(response, offset + 8);
            offset += 10;
            if (offset + dataLength > response.length) {
                return values;
            }
            if (type == TYPE_TXT) {
                int end = offset + dataLength;
                int txtOffset = offset;
                while (txtOffset < end) {
                    int length = response[txtOffset] & 0xff;
                    txtOffset++;
                    if (txtOffset + length > end) {
                        break;
                    }
                    values.add(new String(response, txtOffset, length, StandardCharsets.UTF_8));
                    txtOffset += length;
                }
            }
            offset += dataLength;
        }
        return values;
    }

    private static int skipName(byte[] bytes, int offset) throws IOException {
        while (offset < bytes.length) {
            int length = bytes[offset] & 0xff;
            if ((length & 0xc0) == 0xc0) {
                if (offset + 1 >= bytes.length) {
                    throw new IOException("invalid compressed DNS name");
                }
                return offset + 2;
            }
            if ((length & 0xc0) != 0) {
                throw new IOException("invalid DNS name");
            }
            offset++;
            if (length == 0) {
                return offset;
            }
            offset += length;
        }
        throw new IOException("unterminated DNS name");
    }

    private static int readUint16(byte[] bytes, int offset) throws IOException {
        if (offset + 2 > bytes.length) {
            throw new IOException("truncated DNS message");
        }
        return ((bytes[offset] & 0xff) << 8) | (bytes[offset + 1] & 0xff);
    }

    private static void writeUint16(ByteArrayOutputStream out, int value) {
        out.write((value >>> 8) & 0xff);
        out.write(value & 0xff);
    }

    private record WhoamiQuery(String host, int dnsClass) {
    }
}

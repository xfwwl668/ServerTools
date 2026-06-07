package com.nezhahq.agent.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.64.0)",
    comments = "Source: nezha.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class NezhaServiceGrpc {

  private NezhaServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "proto.NezhaService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.nezhahq.agent.proto.State,
      com.nezhahq.agent.proto.Receipt> getReportSystemStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportSystemState",
      requestType = com.nezhahq.agent.proto.State.class,
      responseType = com.nezhahq.agent.proto.Receipt.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.nezhahq.agent.proto.State,
      com.nezhahq.agent.proto.Receipt> getReportSystemStateMethod() {
    io.grpc.MethodDescriptor<com.nezhahq.agent.proto.State, com.nezhahq.agent.proto.Receipt> getReportSystemStateMethod;
    if ((getReportSystemStateMethod = NezhaServiceGrpc.getReportSystemStateMethod) == null) {
      synchronized (NezhaServiceGrpc.class) {
        if ((getReportSystemStateMethod = NezhaServiceGrpc.getReportSystemStateMethod) == null) {
          NezhaServiceGrpc.getReportSystemStateMethod = getReportSystemStateMethod =
              io.grpc.MethodDescriptor.<com.nezhahq.agent.proto.State, com.nezhahq.agent.proto.Receipt>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportSystemState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.State.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.Receipt.getDefaultInstance()))
              .setSchemaDescriptor(new NezhaServiceMethodDescriptorSupplier("ReportSystemState"))
              .build();
        }
      }
    }
    return getReportSystemStateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.nezhahq.agent.proto.Host,
      com.nezhahq.agent.proto.Receipt> getReportSystemInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportSystemInfo",
      requestType = com.nezhahq.agent.proto.Host.class,
      responseType = com.nezhahq.agent.proto.Receipt.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.nezhahq.agent.proto.Host,
      com.nezhahq.agent.proto.Receipt> getReportSystemInfoMethod() {
    io.grpc.MethodDescriptor<com.nezhahq.agent.proto.Host, com.nezhahq.agent.proto.Receipt> getReportSystemInfoMethod;
    if ((getReportSystemInfoMethod = NezhaServiceGrpc.getReportSystemInfoMethod) == null) {
      synchronized (NezhaServiceGrpc.class) {
        if ((getReportSystemInfoMethod = NezhaServiceGrpc.getReportSystemInfoMethod) == null) {
          NezhaServiceGrpc.getReportSystemInfoMethod = getReportSystemInfoMethod =
              io.grpc.MethodDescriptor.<com.nezhahq.agent.proto.Host, com.nezhahq.agent.proto.Receipt>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportSystemInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.Host.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.Receipt.getDefaultInstance()))
              .setSchemaDescriptor(new NezhaServiceMethodDescriptorSupplier("ReportSystemInfo"))
              .build();
        }
      }
    }
    return getReportSystemInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.nezhahq.agent.proto.TaskResult,
      com.nezhahq.agent.proto.Task> getRequestTaskMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RequestTask",
      requestType = com.nezhahq.agent.proto.TaskResult.class,
      responseType = com.nezhahq.agent.proto.Task.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.nezhahq.agent.proto.TaskResult,
      com.nezhahq.agent.proto.Task> getRequestTaskMethod() {
    io.grpc.MethodDescriptor<com.nezhahq.agent.proto.TaskResult, com.nezhahq.agent.proto.Task> getRequestTaskMethod;
    if ((getRequestTaskMethod = NezhaServiceGrpc.getRequestTaskMethod) == null) {
      synchronized (NezhaServiceGrpc.class) {
        if ((getRequestTaskMethod = NezhaServiceGrpc.getRequestTaskMethod) == null) {
          NezhaServiceGrpc.getRequestTaskMethod = getRequestTaskMethod =
              io.grpc.MethodDescriptor.<com.nezhahq.agent.proto.TaskResult, com.nezhahq.agent.proto.Task>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RequestTask"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.TaskResult.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.Task.getDefaultInstance()))
              .setSchemaDescriptor(new NezhaServiceMethodDescriptorSupplier("RequestTask"))
              .build();
        }
      }
    }
    return getRequestTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.nezhahq.agent.proto.IOStreamData,
      com.nezhahq.agent.proto.IOStreamData> getIOStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "IOStream",
      requestType = com.nezhahq.agent.proto.IOStreamData.class,
      responseType = com.nezhahq.agent.proto.IOStreamData.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.nezhahq.agent.proto.IOStreamData,
      com.nezhahq.agent.proto.IOStreamData> getIOStreamMethod() {
    io.grpc.MethodDescriptor<com.nezhahq.agent.proto.IOStreamData, com.nezhahq.agent.proto.IOStreamData> getIOStreamMethod;
    if ((getIOStreamMethod = NezhaServiceGrpc.getIOStreamMethod) == null) {
      synchronized (NezhaServiceGrpc.class) {
        if ((getIOStreamMethod = NezhaServiceGrpc.getIOStreamMethod) == null) {
          NezhaServiceGrpc.getIOStreamMethod = getIOStreamMethod =
              io.grpc.MethodDescriptor.<com.nezhahq.agent.proto.IOStreamData, com.nezhahq.agent.proto.IOStreamData>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "IOStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.IOStreamData.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.IOStreamData.getDefaultInstance()))
              .setSchemaDescriptor(new NezhaServiceMethodDescriptorSupplier("IOStream"))
              .build();
        }
      }
    }
    return getIOStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.nezhahq.agent.proto.GeoIP,
      com.nezhahq.agent.proto.GeoIP> getReportGeoIPMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportGeoIP",
      requestType = com.nezhahq.agent.proto.GeoIP.class,
      responseType = com.nezhahq.agent.proto.GeoIP.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.nezhahq.agent.proto.GeoIP,
      com.nezhahq.agent.proto.GeoIP> getReportGeoIPMethod() {
    io.grpc.MethodDescriptor<com.nezhahq.agent.proto.GeoIP, com.nezhahq.agent.proto.GeoIP> getReportGeoIPMethod;
    if ((getReportGeoIPMethod = NezhaServiceGrpc.getReportGeoIPMethod) == null) {
      synchronized (NezhaServiceGrpc.class) {
        if ((getReportGeoIPMethod = NezhaServiceGrpc.getReportGeoIPMethod) == null) {
          NezhaServiceGrpc.getReportGeoIPMethod = getReportGeoIPMethod =
              io.grpc.MethodDescriptor.<com.nezhahq.agent.proto.GeoIP, com.nezhahq.agent.proto.GeoIP>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportGeoIP"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.GeoIP.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.GeoIP.getDefaultInstance()))
              .setSchemaDescriptor(new NezhaServiceMethodDescriptorSupplier("ReportGeoIP"))
              .build();
        }
      }
    }
    return getReportGeoIPMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.nezhahq.agent.proto.Host,
      com.nezhahq.agent.proto.Uint64Receipt> getReportSystemInfo2Method;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportSystemInfo2",
      requestType = com.nezhahq.agent.proto.Host.class,
      responseType = com.nezhahq.agent.proto.Uint64Receipt.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.nezhahq.agent.proto.Host,
      com.nezhahq.agent.proto.Uint64Receipt> getReportSystemInfo2Method() {
    io.grpc.MethodDescriptor<com.nezhahq.agent.proto.Host, com.nezhahq.agent.proto.Uint64Receipt> getReportSystemInfo2Method;
    if ((getReportSystemInfo2Method = NezhaServiceGrpc.getReportSystemInfo2Method) == null) {
      synchronized (NezhaServiceGrpc.class) {
        if ((getReportSystemInfo2Method = NezhaServiceGrpc.getReportSystemInfo2Method) == null) {
          NezhaServiceGrpc.getReportSystemInfo2Method = getReportSystemInfo2Method =
              io.grpc.MethodDescriptor.<com.nezhahq.agent.proto.Host, com.nezhahq.agent.proto.Uint64Receipt>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportSystemInfo2"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.Host.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.nezhahq.agent.proto.Uint64Receipt.getDefaultInstance()))
              .setSchemaDescriptor(new NezhaServiceMethodDescriptorSupplier("ReportSystemInfo2"))
              .build();
        }
      }
    }
    return getReportSystemInfo2Method;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static NezhaServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NezhaServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NezhaServiceStub>() {
        @java.lang.Override
        public NezhaServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NezhaServiceStub(channel, callOptions);
        }
      };
    return NezhaServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static NezhaServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NezhaServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NezhaServiceBlockingStub>() {
        @java.lang.Override
        public NezhaServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NezhaServiceBlockingStub(channel, callOptions);
        }
      };
    return NezhaServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static NezhaServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<NezhaServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<NezhaServiceFutureStub>() {
        @java.lang.Override
        public NezhaServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new NezhaServiceFutureStub(channel, callOptions);
        }
      };
    return NezhaServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.State> reportSystemState(
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Receipt> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getReportSystemStateMethod(), responseObserver);
    }

    /**
     */
    default void reportSystemInfo(com.nezhahq.agent.proto.Host request,
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Receipt> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReportSystemInfoMethod(), responseObserver);
    }

    /**
     */
    default io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.TaskResult> requestTask(
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Task> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getRequestTaskMethod(), responseObserver);
    }

    /**
     */
    default io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.IOStreamData> iOStream(
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.IOStreamData> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getIOStreamMethod(), responseObserver);
    }

    /**
     */
    default void reportGeoIP(com.nezhahq.agent.proto.GeoIP request,
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.GeoIP> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReportGeoIPMethod(), responseObserver);
    }

    /**
     */
    default void reportSystemInfo2(com.nezhahq.agent.proto.Host request,
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Uint64Receipt> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReportSystemInfo2Method(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service NezhaService.
   */
  public static abstract class NezhaServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return NezhaServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service NezhaService.
   */
  public static final class NezhaServiceStub
      extends io.grpc.stub.AbstractAsyncStub<NezhaServiceStub> {
    private NezhaServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NezhaServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NezhaServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.State> reportSystemState(
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Receipt> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getReportSystemStateMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void reportSystemInfo(com.nezhahq.agent.proto.Host request,
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Receipt> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReportSystemInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.TaskResult> requestTask(
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Task> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getRequestTaskMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.IOStreamData> iOStream(
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.IOStreamData> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getIOStreamMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void reportGeoIP(com.nezhahq.agent.proto.GeoIP request,
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.GeoIP> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReportGeoIPMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void reportSystemInfo2(com.nezhahq.agent.proto.Host request,
        io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Uint64Receipt> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReportSystemInfo2Method(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service NezhaService.
   */
  public static final class NezhaServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<NezhaServiceBlockingStub> {
    private NezhaServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NezhaServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NezhaServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.nezhahq.agent.proto.Receipt reportSystemInfo(com.nezhahq.agent.proto.Host request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportSystemInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.nezhahq.agent.proto.GeoIP reportGeoIP(com.nezhahq.agent.proto.GeoIP request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportGeoIPMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.nezhahq.agent.proto.Uint64Receipt reportSystemInfo2(com.nezhahq.agent.proto.Host request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportSystemInfo2Method(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service NezhaService.
   */
  public static final class NezhaServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<NezhaServiceFutureStub> {
    private NezhaServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NezhaServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NezhaServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.nezhahq.agent.proto.Receipt> reportSystemInfo(
        com.nezhahq.agent.proto.Host request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReportSystemInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.nezhahq.agent.proto.GeoIP> reportGeoIP(
        com.nezhahq.agent.proto.GeoIP request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReportGeoIPMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.nezhahq.agent.proto.Uint64Receipt> reportSystemInfo2(
        com.nezhahq.agent.proto.Host request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReportSystemInfo2Method(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REPORT_SYSTEM_INFO = 0;
  private static final int METHODID_REPORT_GEO_IP = 1;
  private static final int METHODID_REPORT_SYSTEM_INFO2 = 2;
  private static final int METHODID_REPORT_SYSTEM_STATE = 3;
  private static final int METHODID_REQUEST_TASK = 4;
  private static final int METHODID_IOSTREAM = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REPORT_SYSTEM_INFO:
          serviceImpl.reportSystemInfo((com.nezhahq.agent.proto.Host) request,
              (io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Receipt>) responseObserver);
          break;
        case METHODID_REPORT_GEO_IP:
          serviceImpl.reportGeoIP((com.nezhahq.agent.proto.GeoIP) request,
              (io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.GeoIP>) responseObserver);
          break;
        case METHODID_REPORT_SYSTEM_INFO2:
          serviceImpl.reportSystemInfo2((com.nezhahq.agent.proto.Host) request,
              (io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Uint64Receipt>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REPORT_SYSTEM_STATE:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.reportSystemState(
              (io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Receipt>) responseObserver);
        case METHODID_REQUEST_TASK:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.requestTask(
              (io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.Task>) responseObserver);
        case METHODID_IOSTREAM:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.iOStream(
              (io.grpc.stub.StreamObserver<com.nezhahq.agent.proto.IOStreamData>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getReportSystemStateMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              com.nezhahq.agent.proto.State,
              com.nezhahq.agent.proto.Receipt>(
                service, METHODID_REPORT_SYSTEM_STATE)))
        .addMethod(
          getReportSystemInfoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.nezhahq.agent.proto.Host,
              com.nezhahq.agent.proto.Receipt>(
                service, METHODID_REPORT_SYSTEM_INFO)))
        .addMethod(
          getRequestTaskMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              com.nezhahq.agent.proto.TaskResult,
              com.nezhahq.agent.proto.Task>(
                service, METHODID_REQUEST_TASK)))
        .addMethod(
          getIOStreamMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              com.nezhahq.agent.proto.IOStreamData,
              com.nezhahq.agent.proto.IOStreamData>(
                service, METHODID_IOSTREAM)))
        .addMethod(
          getReportGeoIPMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.nezhahq.agent.proto.GeoIP,
              com.nezhahq.agent.proto.GeoIP>(
                service, METHODID_REPORT_GEO_IP)))
        .addMethod(
          getReportSystemInfo2Method(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.nezhahq.agent.proto.Host,
              com.nezhahq.agent.proto.Uint64Receipt>(
                service, METHODID_REPORT_SYSTEM_INFO2)))
        .build();
  }

  private static abstract class NezhaServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    NezhaServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.nezhahq.agent.proto.NezhaProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("NezhaService");
    }
  }

  private static final class NezhaServiceFileDescriptorSupplier
      extends NezhaServiceBaseDescriptorSupplier {
    NezhaServiceFileDescriptorSupplier() {}
  }

  private static final class NezhaServiceMethodDescriptorSupplier
      extends NezhaServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    NezhaServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (NezhaServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new NezhaServiceFileDescriptorSupplier())
              .addMethod(getReportSystemStateMethod())
              .addMethod(getReportSystemInfoMethod())
              .addMethod(getRequestTaskMethod())
              .addMethod(getIOStreamMethod())
              .addMethod(getReportGeoIPMethod())
              .addMethod(getReportSystemInfo2Method())
              .build();
        }
      }
    }
    return result;
  }
}

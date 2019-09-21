package zio.nio.channels

import java.nio.channels.{ AsynchronousChannelGroup => JAsynchronousChannelGroup }
import java.util.concurrent.{ Executors, TimeUnit, ExecutorService => JExecutorService }

import zio.ZIO
import zio.duration.Duration
import zio.test._
import zio.test.Assertion._

trait ClassFixture {
  def jExecutor: JExecutorService

  def jChannelGroup: JAsynchronousChannelGroup

  def testObj: AsynchronousChannelGroup

  def cleanFixture(): Unit
}

object ClassFixture {

  def apply(): ClassFixture =
    new ClassFixture {
      val jExecutor: JExecutorService = Executors.newFixedThreadPool(1)

      val jChannelGroup: JAsynchronousChannelGroup =
        JAsynchronousChannelGroup.withThreadPool(jExecutor)
      val testObj = new AsynchronousChannelGroup(jChannelGroup)

      def cleanFixture(): Unit = {
        jChannelGroup.shutdown()
        jExecutor.shutdown()
      }
    }

  def providedFixture(xd: ClassFixture => ZIO[Any, Throwable, TestResult]): ZIO[Any, Throwable, TestResult] =
    ZIO(ClassFixture()).bracket(x => ZIO.effectTotal(x.cleanFixture())) { fa =>
      xd(fa)
    }
}

object AsynchronousChannelGroupSpec
    extends ZIOBaseSpec(
      suite("AsynchronousChannelGroupSpec")(
        testM("awaitTermination") {
          ClassFixture.providedFixture { fa =>
            for {
              result <- fa.testObj.awaitTermination(Duration.apply(1, TimeUnit.SECONDS))
            } yield assert(!result, isTrue)
          }
        },
        testM("failing awaitTermination") {
          for {
            result <- new AsynchronousChannelGroup(null)
                       .awaitTermination(Duration.apply(1, TimeUnit.SECONDS))
                       .run
          } yield assert(result, fails(anything))
        },
        testM("isShutdown") {
          ZIO(ClassFixture()).bracket(x => ZIO.effectTotal(x.cleanFixture())) { fa =>
            for {
              result <- fa.testObj.isShutdown
            } yield assert(result, isFalse)
          }
        },
        testM("isTerminated") {
          ZIO(ClassFixture()).bracket(x => ZIO.effectTotal(x.cleanFixture())) { fa =>
            for {
              result <- fa.testObj.isTerminated
            } yield assert(result, isFalse)
          }
        },
        testM("shutdown") {
          ClassFixture.providedFixture { fa =>
            for {
              _ <- fa.testObj.shutdown
            } yield assert(true, isTrue)
          }
        },
        testM("shutdownNow") {
          ClassFixture.providedFixture { fa =>
            for {
              _ <- fa.testObj.shutdownNow
            } yield assert(true, isTrue)
          }
        },
        testM("failing shutdownNow") {
          for {
            result <- new AsynchronousChannelGroup(null).shutdownNow.run
          } yield assert(result, fails(anything))
        },
        testM("companion object create instance using executor and initial size") {
          for {
            result <- AsynchronousChannelGroup(null, 1).run
          } yield assert(result, fails(anything))
        },
        testM("failing companion object") {
          for {
            result <- AsynchronousChannelGroup(null, 1).run
          } yield assert(result, fails(anything))
        },
        testM("failing companion object create instance using threads no and threads factory") {
          for {
            result <- AsynchronousChannelGroup(1, null).run
          } yield assert(result, fails(anything))
        },
        testM("companion object create instance using executor service") {
          for {
            result <- AsynchronousChannelGroup(1, null).run
          } yield assert(result, fails(anything))

        }
      )
    )

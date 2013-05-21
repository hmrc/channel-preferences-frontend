package controller.service

trait DelayResponse {

  import scala.concurrent.duration.Deadline

  protected def delayThread(d: Deadline) {
    while (d.hasTimeLeft()) {
      /* just wasting some cycles */
    }
  }
}

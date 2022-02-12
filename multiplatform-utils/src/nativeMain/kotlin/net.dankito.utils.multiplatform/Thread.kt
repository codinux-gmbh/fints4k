package net.dankito.utils.multiplatform


actual class Thread {

    actual companion object {

        actual val current: Thread
            get() = Thread()


        actual fun printCurrentThreadStackTrace() {
            Thread.current.printStackTrace()
        }

    }


    actual val threadName: String
        get() = "main"


    actual fun printStackTrace() {
        // TODO: find a better way
        Exception("Nothing happened, just to print the stack trace").printStackTrace()
    }

}
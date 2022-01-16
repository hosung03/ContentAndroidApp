package com.visang.mathalive.eventbus

import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

/**
 * EventBus
 *
 * RxAndroid 기반으로 구현한 EventBus 클래스.
 */
object EventBus {

    val observable: Subject<Any> = PublishSubject.create<Any>().toSerialized()

    /**
     * 이벤트 송신
     * @param event 송신할 이벤트 객체 (EventTypes.h에 정의되어 있음)
     */
    fun send(event: Any) {
        observable.onNext(event)
    }

    /**
     * JS Notify 이벤트 송신
     * @param event 웹뷰에 송신할 이벤트 객체
     */
    fun sendJSResult(event: Any) {
        observable.onNext(NotifyJSResult(event))
    }

    fun sendJSCaptureScreenResult(result: Boolean, cbFunc: String? = null, type: String? = "jpg", data: String? = null) {
        observable.onNext(NotifyCaptureScreenResult(result, cbFunc, type, data))
    }

    fun sendJSRecordScreenResult(result: Boolean, cbFunc: String? = null, url: String? = null) {
        observable.onNext(NotifyRecordScreenResult(result, cbFunc, url))
    }

    fun sendJSNotifyDestroy() {
        observable.onNext(NotifyDestroy())
    }

    fun sendJSNotifyError(code: Int) {
        observable.onNext(NotifyError(code))
    }

    fun sendJSNotifyDownloadProgress(result: Boolean, cbFunc: String? = null, target:String, progress: Int, filesize: Int, downloadedfilesize: Int) {
        observable.onNext(NotifyDownloadProgress(result, cbFunc, target, progress, filesize, downloadedfilesize))
    }

}

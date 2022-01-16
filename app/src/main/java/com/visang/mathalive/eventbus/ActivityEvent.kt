package com.visang.mathalive.eventbus

object ActivityEvent {
    class OnStart
    class OnResume
    class OnPause
    class OnStop
    class OnKeyDown(val keyCode: Int)
}

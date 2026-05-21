package com.example.smartpot

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Kakao SDK 초기화
        KakaoSdk.init(this, "91a7ed67032c8c01c8ab18e5486817fd")
    }
}

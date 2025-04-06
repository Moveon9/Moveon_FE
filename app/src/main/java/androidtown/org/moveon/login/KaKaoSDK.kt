package androidtown.org.moveon.login

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class KaKaoSDK : Application(){
    override fun onCreate() {
        super.onCreate()

        KakaoSdk.init(
            context = this,
            appKey = "ba5daeb1c42d6bf9c1ac9b8f6ae78ada"
        )
    }
}
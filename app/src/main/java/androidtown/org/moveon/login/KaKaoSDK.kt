package androidtown.org.moveon.login

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class KaKaoSDK : Application(){
    override fun onCreate() {
        super.onCreate()

        KakaoSdk.init(
            context = this,
            appKey = "api key"
        )
    }
}

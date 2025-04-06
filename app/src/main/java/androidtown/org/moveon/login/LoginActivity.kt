package androidtown.org.moveon.login
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidtown.org.moveon.R
import androidx.appcompat.app.AppCompatActivity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient


class LoginActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val googleLoginButton = findViewById<ImageButton>(R.id.google_login_button)
        val kakaoLoginButton = findViewById<ImageButton>(R.id.kakao_login_button)

        googleLoginButton.setOnClickListener {
            //google 로그인 구현
        }

        kakaoLoginButton.setOnClickListener {
            loginWithKaKao()
        }
    }
    private  fun loginWithKaKao() {
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                handleLoginResult(token, error)
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                handleLoginResult(token, error)
            }
        }
    }
    private fun handleLoginResult(token: OAuthToken?, error: Throwable?){
        if(error != null){
            Log.e("KakaoLogin", "로그인 실패", error)
            Toast.makeText(this, "로그인 실패: ${error.message}", Toast.LENGTH_SHORT).show()
        } else if (token != null) {
            Log.i("KakaoLogin", "로그인 성공: ${token.accessToken}")
            Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()

            fectchUserInfo()
        }
    }
    private fun fectchUserInfo() {
        UserApiClient.instance.me {user, error ->
            if (error != null) {
                Log.e("KakaoLogin", "사용자 정보 요청 실패", error)
            } else if (user != null){
                Log.i("KakaoLogin", "사용자 정보 요청 성공: ${user.kakaoAccount}")

                val requiredScopes = listOf("account_email", "birthday", "gender")
                UserApiClient.instance.loginWithNewScopes(this, requiredScopes) { token, scopeError ->
                    if (scopeError != null){
                        Log.e("KakaoLogin", "추가 동의 실패", scopeError)
                    } else {
                        Log.i("KakaoLogin", "추가 동의 성공: ${token?.scopes}")
                        navigateToNextActivity()
                    }
                }
            }
        }
    }
    private fun navigateToNextActivity() {
        val intent = Intent(this, UserInfoActivity1::class.java)
        startActivity(intent)
        finish()
    }
}
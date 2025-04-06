package androidtown.org.moveon.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidtown.org.moveon.R
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


class UserInfoActivity1 : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_person)

        val nameInput = findViewById<EditText>(R.id.edit_text_name)
        val weightInput = findViewById<EditText>(R.id.edit_text_weight)

        val maleButton = findViewById<Button>(R.id.male_button)
        val femaleButton = findViewById<Button>(R.id.female_button)

        val nextButton = findViewById<TextView>(R.id.next_button)
        var selectedGender: String? = null

        maleButton.setOnClickListener {
            selectedGender = "남성"
            maleButton.setBackgroundResource(R.drawable.login_bg_selected_button)
            femaleButton.setBackgroundResource(R.drawable.login_bg_unselected_button)
        }

        femaleButton.setOnClickListener {
            selectedGender = "여성"
            femaleButton.setBackgroundResource(R.drawable.login_bg_selected_button)
            maleButton.setBackgroundResource(R.drawable.login_bg_unselected_button)
        }

        nextButton.setOnClickListener{
            //다 String 형태로 보냄
            val name = nameInput.text.toString().trim()
            val weight = weightInput.text.toString().trim()

            val selectedPreference = when {
                maleButton.background.constantState == ContextCompat.getDrawable(this, R.drawable.login_bg_selected_button)?.constantState -> "남성"
                femaleButton.background.constantState == ContextCompat.getDrawable(this, R.drawable.login_bg_selected_button)?.constantState -> "여성"
                else -> "선택 안 됨"
            }

            if (name.isEmpty() || weight.isEmpty() || selectedGender == "선택 안 됨") {
                Toast.makeText(this, "모든 항목을 선택해주세요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "이름: $name, 몸무게: $weight, 성별: $selectedGender", Toast.LENGTH_SHORT).show()

            //다음 페이지로 이동 및 서버 전송 구현
            sendDataToNext(name, weight, selectedGender!!)

        }
    }
    private fun sendDataToNext(name: String, weight:String, gender: String) {

        val intent = Intent(this@UserInfoActivity1, UserInfoActivity2::class.java)
        intent.putExtra("NAME", name)
        intent.putExtra("WEIGHT", weight)
        intent.putExtra("GENDER", gender)
        startActivity(intent)
    }
}
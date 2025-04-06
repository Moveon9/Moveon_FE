package androidtown.org.moveon.login

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidtown.org.moveon.MainActivity
import androidtown.org.moveon.R
import androidx.appcompat.app.AppCompatActivity


class UserInfoActivity2 : AppCompatActivity() {
    data class UserInfo(
        val name: String?,
        val weight: Int,
        val gender: String?,
        val age: String,
        val region: String,
        val preference: String
    )
    override fun onCreate(saveInstanceState: Bundle?) {
        super.onCreate( saveInstanceState)
        setContentView(R.layout.activity_login_person2)

        // 앞 activity에서 받아온 데이터
        val name = intent.getStringExtra("NAME")
        val weight = intent.getStringExtra("WEIGHT")
        val gender = intent.getStringExtra("GENDER")

        val nextButton = findViewById<TextView>(R.id.next_button)

        val ageSpinner = findViewById<Spinner>(R.id.age_spinner)
        val regionSpinner = findViewById<Spinner>(R.id.region_spinner)
        val sceneryButton = findViewById<Button>(R.id.scenery_button)
        val exerciseButton = findViewById<Button>(R.id.exercise_button)

        val ageOptions = listOf("10대", "20대", "30대", "40대", "50대", "60대 이상")
        val ageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ageOptions)
        ageSpinner.adapter = ageAdapter

        val regionOptions = listOf("서울", "경기", "부산", "제주", "기타")
        val regionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, regionOptions)
        regionSpinner.adapter = regionAdapter

        var selectedPreference = ""

        sceneryButton.setOnClickListener {
            // "풍경" 버튼 선택: 활성화 배경 설정
            sceneryButton.setBackgroundResource(R.drawable.login_bg_selected_button) // 활성화된 배경
            exerciseButton.setBackgroundResource(R.drawable.login_bg_unselected_button) // 비활성화된 배경
            selectedPreference = "풍경" // 선택된 값을 저장
        }

        exerciseButton.setOnClickListener {
            // "운동" 버튼 선택: 활성화 배경 설정
            exerciseButton.setBackgroundResource(R.drawable.login_bg_selected_button) // 활성화된 배경
            sceneryButton.setBackgroundResource(R.drawable.login_bg_unselected_button) // 비활성화된 배경
            selectedPreference = "운동" // 선택된 값을 저장
        }

        nextButton.setOnClickListener{
            val selectedAge = ageSpinner.selectedItem.toString()
            val selectedRegion = regionSpinner.selectedItem.toString()


            if (selectedAge.isEmpty() || selectedRegion.isEmpty() || selectedPreference == null) {
                Toast.makeText(this, "모든 항목을 선택해주세요!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "나이: $selectedAge, 지역: $selectedRegion, 선호도: $selectedPreference", Toast.LENGTH_SHORT).show()

            sendDataToServer(name, weight, gender, selectedAge, selectedRegion, selectedPreference)
        }

        /**********************
            이 뒤에 선택된 값들 서버로 보내는 로직 구현
         **********************/
    }
    private fun sendDataToServer(name: String?, weight: String?, gender: String?, age: String, region: String, preference: String){
        val userInfo = UserInfo(
            name = name,
            weight = weight?.toIntOrNull() ?: 0,
            gender = gender,
            age = age,
            region = region,
            preference = preference
        )

        val intent = Intent(this@UserInfoActivity2, MainActivity::class.java)
        startActivity(intent)
    }
}
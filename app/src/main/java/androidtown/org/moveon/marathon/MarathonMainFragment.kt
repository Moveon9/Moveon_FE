package androidtown.org.moveon.marathon

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.launch
import androidtown.org.moveon.R
import java.util.Locale

class MarathonMainFragment : Fragment(), OnMapReadyCallback {

    // UI 요소 초기화
    private lateinit var mapView: MapView
    private lateinit var startButton: Button
    private lateinit var locationTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var infoBox: View

    // 지도 관련 변수
    private lateinit var naverMap: NaverMap
    private lateinit var pathOverlay: PathOverlay
    private var coordList: List<LatLng> = emptyList()

    // 위치 관련 변수
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // 상태 변수
    private var isRunningStarted = false
    private var startLat = 37.566609
    private var startLng = 126.978371

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    // 뷰 생성
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_marathon_main, container, false)
    }

    // 뷰 생성 후 초기화 작업
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById(R.id.mapView)
        startButton = view.findViewById(R.id.btnStart)
        locationTextView = view.findViewById(R.id.textCurrentLocation)
        distanceTextView = view.findViewById(R.id.textRemainingDistance)
        infoBox = view.findViewById(R.id.infoBox)
        infoBox.visibility = View.GONE

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        startButton.setOnClickListener { checkDistanceToStart() }

        arguments?.let {
            startLat = it.getDouble("startLat", startLat)
            startLng = it.getDouble("startLng", startLng)
        }
    }

    // 지도 로딩 완료 후 호출
    override fun onMapReady(map: NaverMap) {
        naverMap = map
        if (checkLocationPermission()) {
            naverMap.locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
            naverMap.uiSettings.isLocationButtonEnabled = true
            naverMap.locationOverlay.isVisible = true
        }
        requestDirectionAndDrawPath()
    }

    // 출발지까지의 거리 확인 및 출발 조건 체크
    private fun checkDistanceToStart() {
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val distance = calculateDistanceInMeters(startLat, startLng, it.latitude, it.longitude)
                if (distance < 30) {
                    isRunningStarted = true
                    startButton.visibility = View.GONE
                    Toast.makeText(requireContext(), "출발합니다!", Toast.LENGTH_SHORT).show()
                    infoBox.visibility = View.VISIBLE
                    updateLocationUI(it.latitude, it.longitude)
                    startTracking()
                } else showMoveToStartPopup()
            } ?: Toast.makeText(requireContext(), "위치 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 현재 위치 및 남은 거리 UI 업데이트
    private fun updateLocationUI(lat: Double, lng: Double) {
        val geocoder = Geocoder(requireContext(), Locale.KOREA)
        locationTextView.text = try {
            val address = geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
            val city = simplifyCityName(address?.adminArea)
            val district = address?.subLocality
            when {
                city != null && district != null -> "$city $district "
                district != null -> district
                else -> "위치 불명"
            }
        } catch (e: Exception) {
            "위치 불명"
        }

        val goalLat = arguments?.getDouble("goalLat", 37.497942) ?: 37.497942
        val goalLng = arguments?.getDouble("goalLng", 127.027621) ?: 127.027621
        val remaining = calculateDistanceInMeters(lat, lng, goalLat, goalLng)
        distanceTextView.text = "/ %.1fkm".format(remaining / 1000.0)
    }

    // 위치 추적 시작
    private fun startTracking() {
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val userLatLng = LatLng(location.latitude, location.longitude)
                updateLocationUI(location.latitude, location.longitude)
                updateRemainingPath(userLatLng)
            }
        }

        val request = LocationRequest.create().apply {
            interval = 3000
            fastestInterval = 1500
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    // 현재 위치 기준으로 경로 줄이기
    private fun updateRemainingPath(currentLocation: LatLng) {
        if (!::pathOverlay.isInitialized || coordList.isEmpty()) return
        val remainingPath = coordList.dropWhile { it.distanceTo(currentLocation) > 20.0 }
        if (remainingPath.size >= 2) pathOverlay.coords = remainingPath
    }

    // 방향 요청 후 지도에 경로 표시
    private fun requestDirectionAndDrawPath() {
        val startLat = arguments?.getDouble("startLat", 37.566609) ?: 37.566609
        val startLng = arguments?.getDouble("startLng", 126.978371) ?: 126.978371
        val goalLat = arguments?.getDouble("goalLat", 37.497942) ?: 37.497942
        val goalLng = arguments?.getDouble("goalLng", 127.027621) ?: 127.027621

        val start = "$startLng,$startLat"
        val goal = "$goalLng,$goalLat"
        val startPos = LatLng(startLat, startLng)
        val goalPos = LatLng(goalLat, goalLng)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getDirection(
                    clientId = "szv2eoxq9s",
                    clientSecret = "3HgngSCA6dSeJv8aSX1FlW9Mgm9upwOCaGIdMgtd",
                    start = start,
                    goal = goal,
                    option = "traoptimal"
                )

                if (response.isSuccessful) {
                    response.body()?.route?.traoptimal?.firstOrNull()?.path?.let { path ->
                        coordList = path.map { LatLng(it[1], it[0]) }
                        pathOverlay = PathOverlay().apply {
                            coords = coordList
                            color = Color.BLUE
                            width = 10
                            map = naverMap
                        }
                        Marker().apply { position = startPos; captionText = "출발"; map = naverMap }
                        Marker().apply { position = goalPos; captionText = "도착"; map = naverMap }
                        naverMap.moveCamera(CameraUpdate.scrollTo(coordList.first()))
                    }
                } else Log.e("DirectionAPI", "API 실패: ${response.code()}")
            } catch (e: Exception) {
                Log.e("DirectionAPI", "에러: ${e.message}")
            }
        }
    }

    // 출발지로 이동 안내 팝업
    private fun showMoveToStartPopup() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_go_to_start, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // 두 지점 거리 계산
    private fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val start = Location("").apply { latitude = lat1; longitude = lon1 }
        val end = Location("").apply { latitude = lat2; longitude = lon2 }
        return start.distanceTo(end)
    }

    // 위치 권한 확인
    private fun checkLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // 위치 권한 요청
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // 위치 권한 결과 처리
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "위치 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
                checkDistanceToStart()
            } else {
                Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 시/도 이름 매핑
    private fun simplifyCityName(adminArea: String?): String {
        return when {
            adminArea == null -> ""
            adminArea.contains("서울") -> "서울시"
            adminArea.contains("부산") -> "부산시"
            adminArea.contains("대구") -> "대구시"
            adminArea.contains("인천") -> "인천시"
            adminArea.contains("광주") -> "광주시"
            adminArea.contains("대전") -> "대전시"
            adminArea.contains("울산") -> "울산시"
            adminArea.contains("세종") -> "세종시"
            else -> adminArea
        }
    }
    // Fragment 생명주기 - 지도 생명주기와 동기화
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onDestroyView() {
        if (::locationCallback.isInitialized) fusedLocationClient.removeLocationUpdates(locationCallback)
        mapView.onDestroy()
        super.onDestroyView()
    }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}
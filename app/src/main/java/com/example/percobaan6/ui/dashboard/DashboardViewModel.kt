package com.example.percobaan6.ui.dashboard

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

data class ProfileData(
    val title: String = "Dr.",
    val firstName: String = "Marie",
    val lastName: String = "Marshall",
    val nic: String = "810264320 V",
    val gender: String = "Female",
    val dateOfBirth: String = "05-03-1990",
    val phoneNumber: String = "+62 812-3456-7890",
    val emergencyContact1: String = "+62 811-2345-6789",
    val emergencyContact2: String = "+62 813-4567-8901",
    val email: String = "marie.marshall@email.com"
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    private val _profileData = MutableLiveData<ProfileData>().apply {
        value = loadProfileData()
    }
    val profileData: LiveData<ProfileData> = _profileData

    private val _profilePhotoUri = MutableLiveData<String>().apply {
        value = loadProfilePhotoUri()
    }
    val profilePhotoUri: LiveData<String> = _profilePhotoUri

    // Load data dari SharedPreferences
    private fun loadProfileData(): ProfileData {
        return ProfileData(
            title = sharedPreferences.getString("title", "Dr.") ?: "Dr.",
            firstName = sharedPreferences.getString("firstName", "Marie") ?: "Marie",
            lastName = sharedPreferences.getString("lastName", "Marshall") ?: "Marshall",
            nic = sharedPreferences.getString("nic", "810264320 V") ?: "810264320 V",
            gender = sharedPreferences.getString("gender", "Female") ?: "Female",
            dateOfBirth = sharedPreferences.getString("dateOfBirth", "05-03-1990") ?: "05-03-1990",
            phoneNumber = sharedPreferences.getString("phoneNumber", "+62 812-3456-7890") ?: "+62 812-3456-7890",
            emergencyContact1 = sharedPreferences.getString("emergencyContact1", "+62 811-2345-6789") ?: "+62 811-2345-6789",
            emergencyContact2 = sharedPreferences.getString("emergencyContact2", "+62 813-4567-8901") ?: "+62 813-4567-8901",
            email = sharedPreferences.getString("email", "marie.marshall@email.com") ?: "marie.marshall@email.com"
        )
    }

    private fun loadProfilePhotoUri(): String {
        return sharedPreferences.getString("profilePhotoUri", "") ?: ""
    }

    // Simpan data ke SharedPreferences
    private fun saveProfileData(profileData: ProfileData) {
        with(sharedPreferences.edit()) {
            putString("title", profileData.title)
            putString("firstName", profileData.firstName)
            putString("lastName", profileData.lastName)
            putString("nic", profileData.nic)
            putString("gender", profileData.gender)
            putString("dateOfBirth", profileData.dateOfBirth)
            putString("phoneNumber", profileData.phoneNumber)
            putString("emergencyContact1", profileData.emergencyContact1)
            putString("emergencyContact2", profileData.emergencyContact2)
            putString("email", profileData.email)
            apply()
        }
    }

    private fun saveProfilePhotoUri(uri: String) {
        with(sharedPreferences.edit()) {
            putString("profilePhotoUri", uri)
            apply()
        }
    }

    // Update functions for profile data
    fun updateTitle(title: String) {
        val currentProfile = _profileData.value ?: ProfileData()
        val updatedProfile = currentProfile.copy(title = title)
        _profileData.value = updatedProfile
        saveProfileData(updatedProfile)
    }

    fun updateFirstName(firstName: String) {
        val currentProfile = _profileData.value ?: ProfileData()
        val updatedProfile = currentProfile.copy(firstName = firstName)
        _profileData.value = updatedProfile
        saveProfileData(updatedProfile)
    }

    fun updateLastName(lastName: String) {
        val currentProfile = _profileData.value ?: ProfileData()
        val updatedProfile = currentProfile.copy(lastName = lastName)
        _profileData.value = updatedProfile
        saveProfileData(updatedProfile)
    }

    fun updateNic(nic: String) {
        val currentProfile = _profileData.value ?: ProfileData()
        val updatedProfile = currentProfile.copy(nic = nic)
        _profileData.value = updatedProfile
        saveProfileData(updatedProfile)
    }

    fun updateGender(gender: String) {
        val currentProfile = _profileData.value ?: ProfileData()
        val updatedProfile = currentProfile.copy(gender = gender)
        _profileData.value = updatedProfile
        saveProfileData(updatedProfile)
    }

    fun updateDateOfBirth(dateOfBirth: String) {
        val currentProfile = _profileData.value ?: ProfileData()
        val updatedProfile = currentProfile.copy(dateOfBirth = dateOfBirth)
        _profileData.value = updatedProfile
        saveProfileData(updatedProfile)
    }

    fun updatePhoneNumber(phoneNumber: String) {
        val currentProfile = _profileData.value ?: ProfileData()
        val updatedProfile = currentProfile.copy(phoneNumber = phoneNumber)
        _profileData.value = updatedProfile
        saveProfileData(updatedProfile)
    }

    fun updateEmergencyContact1(emergencyContact1: String) {
        val currentProfile = _profileData.value ?: ProfileData()
        val updatedProfile = currentProfile.copy(emergencyContact1 = emergencyContact1)
        _profileData.value = updatedProfile
        saveProfileData(updatedProfile)
    }

    fun updateEmergencyContact2(emergencyContact2: String) {
        val currentProfile = _profileData.value ?: ProfileData()
        val updatedProfile = currentProfile.copy(emergencyContact2 = emergencyContact2)
        _profileData.value = updatedProfile
        saveProfileData(updatedProfile)
    }

    fun updateEmail(email: String) {
        val currentProfile = _profileData.value ?: ProfileData()
        val updatedProfile = currentProfile.copy(email = email)
        _profileData.value = updatedProfile
        saveProfileData(updatedProfile)
    }

    fun updateProfilePhoto(uri: String) {
        _profilePhotoUri.value = uri
        saveProfilePhotoUri(uri)
    }
}
//package com.example.percobaan6.ui.dashboard
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//
//class DashboardViewModel : ViewModel() {
//
//    private val _text = MutableLiveData<String>().apply {
//        value = "This is dashboard Fragment"
//    }
//    val text: LiveData<String> = _text
//}
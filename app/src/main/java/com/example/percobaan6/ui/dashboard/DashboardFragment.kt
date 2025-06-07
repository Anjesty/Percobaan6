package com.example.percobaan6.ui.dashboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.percobaan6.R
import com.example.percobaan6.databinding.FragmentDashboardBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var dashboardViewModel: DashboardViewModel

    // Activity result launcher untuk memilih foto dari galeri
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                // Copy gambar ke internal storage
                val internalUri = copyImageToInternalStorage(it)
                if (internalUri != null) {
                    binding.ivProfilePhoto.setImageURI(internalUri)
                    dashboardViewModel.updateProfilePhoto(internalUri.toString())
                } else {
                    Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupObservers()
        setupClickListeners()
        setupTabSwitching()

        return root
    }

    private fun setupObservers() {
        // Observe profile data
        dashboardViewModel.profileData.observe(viewLifecycleOwner) { profile ->
            binding.tvUserName.text = "${profile.title} ${profile.firstName} ${profile.lastName}"
            binding.tvTitle.text = profile.title
            binding.tvFirstName.text = profile.firstName
            binding.tvLastName.text = profile.lastName
            binding.tvNic.text = profile.nic
            binding.tvGender.text = profile.gender
            binding.tvDateOfBirth.text = profile.dateOfBirth
            binding.tvPhoneNumber.text = profile.phoneNumber
            binding.tvEmergencyContact1.text = profile.emergencyContact1
            binding.tvEmergencyContact2.text = profile.emergencyContact2
            binding.tvEmail.text = profile.email
        }

        // Observe profile photo
        dashboardViewModel.profilePhotoUri.observe(viewLifecycleOwner) { uri ->
            if (uri.isNotEmpty()) {
                try {
                    binding.ivProfilePhoto.setImageURI(Uri.parse(uri))
                } catch (e: Exception) {
                    // Jika gagal load gambar, gunakan default
                    binding.ivProfilePhoto.setImageResource(R.drawable.default_profile)
                }
            }
        }
    }

    private fun copyImageToInternalStorage(sourceUri: Uri): Uri? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(sourceUri)
            val fileName = "profile_photo_${System.currentTimeMillis()}.jpg"
            val file = File(requireContext().filesDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            Uri.fromFile(file)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun setupClickListeners() {
        // Close button
        binding.btnClose.setOnClickListener {
            // Navigasi kembali atau tutup fragment
            requireActivity().onBackPressed()
        }

        // Edit Photo button
        binding.btnEditPhoto.setOnClickListener {
            openImagePicker()
        }

        // Basic Details Click Listeners
        binding.titleLayout.setOnClickListener {
            showEditDialog("Title", dashboardViewModel.profileData.value?.title ?: "") { newValue ->
                dashboardViewModel.updateTitle(newValue)
            }
        }

        binding.firstNameLayout.setOnClickListener {
            showEditDialog("First Name", dashboardViewModel.profileData.value?.firstName ?: "") { newValue ->
                dashboardViewModel.updateFirstName(newValue)
            }
        }

        binding.lastNameLayout.setOnClickListener {
            showEditDialog("Last Name", dashboardViewModel.profileData.value?.lastName ?: "") { newValue ->
                dashboardViewModel.updateLastName(newValue)
            }
        }

        binding.nicLayout.setOnClickListener {
            showEditDialog("NIC", dashboardViewModel.profileData.value?.nic ?: "") { newValue ->
                dashboardViewModel.updateNic(newValue)
            }
        }

        binding.genderLayout.setOnClickListener {
            showGenderDialog()
        }

        binding.dobLayout.setOnClickListener {
            showDatePickerDialog()
        }

        // Contact Details Click Listeners
        binding.phoneLayout.setOnClickListener {
            showEditDialog("Phone Number", dashboardViewModel.profileData.value?.phoneNumber ?: "") { newValue ->
                dashboardViewModel.updatePhoneNumber(newValue)
            }
        }

        binding.emergencyContact1Layout.setOnClickListener {
            showEditDialog("Emergency Contact 1", dashboardViewModel.profileData.value?.emergencyContact1 ?: "") { newValue ->
                dashboardViewModel.updateEmergencyContact1(newValue)
            }
        }

        binding.emergencyContact2Layout.setOnClickListener {
            showEditDialog("Emergency Contact 2", dashboardViewModel.profileData.value?.emergencyContact2 ?: "") { newValue ->
                dashboardViewModel.updateEmergencyContact2(newValue)
            }
        }

        binding.emailLayout.setOnClickListener {
            showEditDialog("Email", dashboardViewModel.profileData.value?.email ?: "") { newValue ->
                dashboardViewModel.updateEmail(newValue)
            }
        }
    }

    private fun setupTabSwitching() {
        binding.btnBasicDetails.setOnClickListener {
            switchToBasicDetails()
        }

        binding.btnContactDetails.setOnClickListener {
            switchToContactDetails()
        }
    }

    private fun switchToBasicDetails() {
        // Update tab appearance
        binding.btnBasicDetails.setBackgroundResource(R.drawable.tab_selected_background)
        binding.btnBasicDetails.setTextColor(resources.getColor(R.color.text_color, null))
        binding.btnContactDetails.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.btnContactDetails.setTextColor(resources.getColor(android.R.color.darker_gray, null))

        // Show/hide content
        binding.basicDetailsContent.visibility = View.VISIBLE
        binding.contactDetailsContent.visibility = View.GONE
    }

    private fun switchToContactDetails() {
        // Update tab appearance
        binding.btnContactDetails.setBackgroundResource(R.drawable.tab_selected_background)
        binding.btnContactDetails.setTextColor(resources.getColor(R.color.text_color, null))
        binding.btnBasicDetails.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.btnBasicDetails.setTextColor(resources.getColor(android.R.color.darker_gray, null))

        // Show/hide content
        binding.basicDetailsContent.visibility = View.GONE
        binding.contactDetailsContent.visibility = View.VISIBLE
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun showEditDialog(title: String, currentValue: String, onSave: (String) -> Unit) {
        val editText = EditText(requireContext()).apply {
            setText(currentValue)
            setSelection(text.length)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit $title")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newValue = editText.text.toString().trim()
                if (newValue.isNotEmpty()) {
                    onSave(newValue)
                    Toast.makeText(requireContext(), "$title updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "$title cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGenderDialog() {
        val genders = arrayOf("Male", "Female", "Other")
        val currentGender = dashboardViewModel.profileData.value?.gender ?: "Female"
        val selectedIndex = genders.indexOf(currentGender)

        AlertDialog.Builder(requireContext())
            .setTitle("Select Gender")
            .setSingleChoiceItems(genders, selectedIndex) { dialog, which ->
                dashboardViewModel.updateGender(genders[which])
                Toast.makeText(requireContext(), "Gender updated successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePickerDialog() {
        val currentDate = dashboardViewModel.profileData.value?.dateOfBirth ?: "05-03-1990"
        val dateParts = currentDate.split("-")

        val day = if (dateParts.size >= 1) dateParts[0].toIntOrNull() ?: 5 else 5
        val month = if (dateParts.size >= 2) dateParts[1].toIntOrNull() ?: 3 else 3
        val year = if (dateParts.size >= 3) dateParts[2].toIntOrNull() ?: 1990 else 1990

        val datePickerDialog = android.app.DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear)
                dashboardViewModel.updateDateOfBirth(formattedDate)
                Toast.makeText(requireContext(), "Date of birth updated successfully", Toast.LENGTH_SHORT).show()
            },
            year, month - 1, day
        )

        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
//package com.example.percobaan6.ui.dashboard
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import com.example.percobaan6.databinding.FragmentDashboardBinding
//
//class DashboardFragment : Fragment() {
//
//    private var _binding: FragmentDashboardBinding? = null
//
//    // This property is only valid between onCreateView and
//    // onDestroyView.
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        val dashboardViewModel =
//            ViewModelProvider(this).get(DashboardViewModel::class.java)
//
//        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
//        val root: View = binding.root
//
//        val textView: TextView = binding.textDashboard
//        dashboardViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
//        return root
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
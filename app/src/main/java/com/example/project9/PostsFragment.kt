package com.example.project9

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.example.project9.databinding.FragmentPostsBinding
import edu.iu.habahram.simpleinsta.AndroidSensor
import java.io.File
import java.io.IOException
import java.util.UUID








class PostsFragment : Fragment() {
    private var accelerometerData = floatArrayOf(
        SensorManager.GRAVITY_EARTH, SensorManager.GRAVITY_EARTH, 0.0F
    )

    val TAG = "PostsFragment"
    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!

    var camActivityResultLauncher: ActivityResultLauncher<Uri>? = null

    private var photoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPostsBinding.inflate(inflater, container, false)
        val view = binding.root
        val viewModel : PostsViewModel by activityViewModels()
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val adapter = PostsAdapter(this.requireContext())
        binding.rvPosts.adapter = adapter
        viewModel.posts.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })

        val sens = AccelerometerSensor(this.requireContext())
        sens.setOnSensorValuesChangedListener {a ->
            val x: Float = a[0]
            val y: Float = a[1]
            val z: Float = a[2]
            accelerometerData[1] = accelerometerData[0]
            accelerometerData[0] = Math.sqrt((x * x).toDouble() + y * y + z * z).toFloat()
            val delta: Float = accelerometerData[0] - accelerometerData[1]
            accelerometerData[2] = accelerometerData[2] * 0.9f + delta
            if (accelerometerData[2] > 12) {
                // this is when the phone is shaken a sufficient amount
                OnPhoneShaken()
            }
        }
        // make sure our sensor is listening for sensor values changed
        sens.startListening()

        // register activity
        camActivityResultLauncher = registerForActivityResult<Uri, Boolean>(
            ActivityResultContracts.TakePicture()) {
            val viewModel: PostsViewModel by activityViewModels()
            viewModel.AddNewPicture(photoUri!!)
        }

        // logout by going back to login screen
        binding.bLogout.setOnClickListener {
            view.findNavController().navigate(R.id.action_postsFragment_to_loginFragment)
        }

        // upload image from camera roll
        binding.fabCreate.setOnClickListener {
          view.findNavController().navigate(R.id.action_postsFragment_to_createFragment)
        }
        return view
    }

    fun OnPhoneShaken()
    {
        val outputDir = requireContext().cacheDir // context being the Activity pointer
        val outputFile = File.createTempFile("tmp", ".jpg", outputDir)
        val uri = FileProvider.getUriForFile(requireContext(), "com.package.name.fileprovider", outputFile)
        photoUri = uri
        // launch the camera with a temporary file uri
        camActivityResultLauncher?.launch(uri)
    }

}

// to get accelerometer values
class AccelerometerSensor(
    context: Context
): AndroidSensor(
    context = context,
    sensorFeature = PackageManager.FEATURE_SENSOR_ACCELEROMETER,
    sensorType = Sensor.TYPE_ACCELEROMETER
)
package com.android.anupkumar.instagram.ui.photo

import androidx.lifecycle.MutableLiveData
import com.android.anupkumar.instagram.R
import com.android.anupkumar.instagram.data.model.Post
import com.android.anupkumar.instagram.data.model.User
import com.android.anupkumar.instagram.data.repository.PhotoRepository
import com.android.anupkumar.instagram.data.repository.PostRepository
import com.android.anupkumar.instagram.data.repository.UserRepository
import com.android.anupkumar.instagram.ui.base.BaseViewModel
import com.android.anupkumar.instagram.utils.common.Event
import com.android.anupkumar.instagram.utils.common.FileUtils
import com.android.anupkumar.instagram.utils.common.Resource
import com.android.anupkumar.instagram.utils.log.Logger
import com.android.anupkumar.instagram.utils.network.NetworkHelper
import com.android.anupkumar.instagram.utils.rx.SchedulerProvider
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.io.File
import java.io.InputStream

class PhotoViewModel(
    schedulerProvider: SchedulerProvider,
    compositeDisposable: CompositeDisposable,
    private val userRepository: UserRepository,
    private val photoRepository: PhotoRepository,
    private val postRepository: PostRepository,
    networkHelper: NetworkHelper,
    private val directory: File
) : BaseViewModel(schedulerProvider, compositeDisposable, networkHelper) {

    private val user: User = userRepository.getCurrentUser()!! // should not be used without logged in user

    val loading: MutableLiveData<Boolean> = MutableLiveData()
    val post: MutableLiveData<Event<Post>> = MutableLiveData()

    override fun onCreate() {}

    fun onGalleryImageSelected(inputStream: InputStream) {
        loading.postValue(true)
        compositeDisposable.add(
            Single.fromCallable {
                FileUtils.saveInputStreamToFile(
                    inputStream, directory, "gallery_img_temp", 500
                )
            }
                .subscribeOn(schedulerProvider.io())
                .subscribe(
                    {
                        if (it != null) {
                            FileUtils.getImageSize(it)?.run {
                                uploadPhotoAndCreatePost(it, this)
                            }
                        } else {
                            loading.postValue(false)
                            messageStringId.postValue(Resource.error(R.string.try_again))

                        }
                    },
                    {
                        loading.postValue(false)
                        messageStringId.postValue(Resource.error(R.string.try_again))
                    }
                )
        )
    }

    fun onCameraImageTaken(cameraImageProcessor: () -> String) {
        loading.postValue(true)
        compositeDisposable.add(
            Single.fromCallable { cameraImageProcessor() }
                .subscribeOn(schedulerProvider.io())
                .subscribe(
                    {
                        File(it).apply {
                            FileUtils.getImageSize(this)?.let { size ->
                                uploadPhotoAndCreatePost(this, size)
                            } ?: loading.postValue(false)
                        }
                    },
                    {
                        loading.postValue(false)
                        messageStringId.postValue(Resource.error(R.string.try_again))
                    }
                )
        )
    }

    private fun uploadPhotoAndCreatePost(imageFile: File, imageSize: Pair<Int, Int>) {
        Logger.d("DEBUG", imageFile.path)
        compositeDisposable.add(
            photoRepository.uploadPhoto(imageFile, user)
                .flatMap {
                    postRepository.createPost(it, imageSize.first, imageSize.second, user)
                }
                .subscribeOn(schedulerProvider.io())
                .subscribe(
                    {
                        loading.postValue(false)
                        post.postValue(Event(it))
                    },
                    {
                        handleNetworkError(it)
                        loading.postValue(false)
                    }
                )

        )
    }
}
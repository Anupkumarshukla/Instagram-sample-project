package com.android.anupkumar.instagram.ui.home

import androidx.lifecycle.MutableLiveData
import com.android.anupkumar.instagram.data.model.Post
import com.android.anupkumar.instagram.data.model.User
import com.android.anupkumar.instagram.data.repository.PostRepository
import com.android.anupkumar.instagram.data.repository.UserRepository
import com.android.anupkumar.instagram.ui.base.BaseViewModel
import com.android.anupkumar.instagram.utils.common.Resource
import com.android.anupkumar.instagram.utils.network.NetworkHelper
import com.android.anupkumar.instagram.utils.rx.SchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers

class HomeViewModel(
    schedulerProvider: SchedulerProvider,
    compositeDisposable: CompositeDisposable,
    networkHelper: NetworkHelper,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val allPostList: ArrayList<Post>,
    private val paginator: PublishProcessor<Pair<String?, String?>>
) : BaseViewModel(schedulerProvider, compositeDisposable, networkHelper) {

    val loading: MutableLiveData<Boolean> = MutableLiveData()
    val posts: MutableLiveData<Resource<List<Post>>> = MutableLiveData()
    val refreshPosts: MutableLiveData<Resource<List<Post>>> = MutableLiveData()

    var firstId: String? = null
    var lastId: String? = null

    private val user: User = userRepository.getCurrentUser()!! // should not be used without logged in user

    init {
        compositeDisposable.add(
            paginator
                .onBackpressureDrop()
                .doOnNext {
                    loading.postValue(true)
                }
                .concatMapSingle { pageIds ->
                    return@concatMapSingle postRepository
                        .fetchHomePostList(pageIds.first, pageIds.second, user)
                        .subscribeOn(Schedulers.io())
                        .doOnError {
                            loading.postValue(false)
                            handleNetworkError(it)
                        }
                }
                .subscribe(
                    {
                        allPostList.addAll(it)

                        firstId = allPostList.maxBy { post -> post.createdAt.time }?.id
                        lastId = allPostList.minBy { post -> post.createdAt.time }?.id

                        loading.postValue(false)
                        posts.postValue(Resource.success(it))
                    },
                    {
                        loading.postValue(false)
                        handleNetworkError(it)
                    }
                )
        )
    }

    override fun onCreate() {
        loadMorePosts()
    }

    private fun loadMorePosts() {
        if (checkInternetConnectionWithMessage()) paginator.onNext(Pair(firstId, lastId))
    }

    fun onLoadMore() {
        if (loading.value !== null && loading.value == false) loadMorePosts()
    }

    fun onNewPost(post: Post) {
        allPostList.add(0, post)
        refreshPosts.postValue(Resource.success(mutableListOf<Post>().apply { addAll(allPostList) }))
    }
}
package com.android.anupkumar.instagram.ui.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.android.anupkumar.instagram.R
import com.android.anupkumar.instagram.data.model.User
import com.android.anupkumar.instagram.data.repository.UserRepository
import com.android.anupkumar.instagram.utils.common.Event
import com.android.anupkumar.instagram.utils.common.Resource
import com.android.anupkumar.instagram.utils.network.NetworkHelper
import com.android.anupkumar.instagram.utils.rx.TestSchedulerProvider
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class LoginViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    private lateinit var networkHelper: NetworkHelper

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var loggingInObserver: Observer<Boolean>

    @Mock
    private lateinit var launchDummyObserver: Observer<Event<Map<String, String>>>

    @Mock
    private lateinit var messageStringIdObserver: Observer<Resource<Int>>

    private lateinit var testScheduler: TestScheduler

    private lateinit var loginViewModel: LoginViewModel

    @Before
    fun setUp() {
        val compositeDisposable = CompositeDisposable()
        testScheduler = TestScheduler()
        val testSchedulerProvider = TestSchedulerProvider(testScheduler)
        loginViewModel = LoginViewModel(
            testSchedulerProvider,
            compositeDisposable,
            networkHelper,
            userRepository
        )
        loginViewModel.loggingIn.observeForever(loggingInObserver)
        loginViewModel.launchMain.observeForever(launchDummyObserver)
        loginViewModel.messageStringId.observeForever(messageStringIdObserver)
    }

    @Test
    fun givenServerResponse200_whenLogin_shouldLaunchDummyActivity() {
        val email = "test@gmail.com"
        val password = "password"
        val user = User("id", "test", email, "accessToken")
        loginViewModel.emailField.value = email
        loginViewModel.passwordField.value = password
        doReturn(true)
            .`when`(networkHelper)
            .isNetworkConnected()
        doReturn(Single.just(user))
            .`when`(userRepository)
            .doUserLogin(email, password)
        loginViewModel.onLogin()
        testScheduler.triggerActions()
        verify(userRepository).saveCurrentUser(user)
        assert(loginViewModel.loggingIn.value == false)
        assert(loginViewModel.launchMain.value == Event(hashMapOf<String, String>()))
        verify(loggingInObserver).onChanged(true)
        verify(loggingInObserver).onChanged(false)
        verify(launchDummyObserver).onChanged(Event(hashMapOf()))
    }

    @Test
    fun givenNoInternet_whenLogin_shouldShowNetworkError() {
        val email = "test@gmail.com"
        val password = "password"
        loginViewModel.emailField.value = email
        loginViewModel.passwordField.value = password
        doReturn(false)
            .`when`(networkHelper)
            .isNetworkConnected()
        loginViewModel.onLogin()
        assert(loginViewModel.messageStringId.value == Resource.error(R.string.network_connection_error))
        verify(messageStringIdObserver).onChanged(Resource.error(R.string.network_connection_error))
    }

    @After
    fun tearDown() {
        loginViewModel.loggingIn.removeObserver(loggingInObserver)
        loginViewModel.launchMain.removeObserver(launchDummyObserver)
        loginViewModel.messageStringId.removeObserver(messageStringIdObserver)
    }

}
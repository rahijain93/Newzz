package com.akash.newsapp.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.akash.newsapp.adapters.ArticleRowViewModel
import com.akash.newsapp.base.BaseRowModel
import com.akash.newsapp.base.Event
import com.akash.newsapp.categoryconstants.Category
import com.akash.newsapp.data.repositories.NewsRepository
import kotlinx.coroutines.*

class ArticleViewModel constructor(private val newsRepository: NewsRepository) : ViewModel(),ErrorState.ErrorStateRetryListener {
    private val TAG = ArticleViewModel::class.java.simpleName

    private val job = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.Main) + job
    private val _event = MutableLiveData<Event<ViewEvent>>()
    val event = _event
    var category : String = Category.GENERAL

    private val _errorState = ErrorState(this as ErrorState.ErrorStateRetryListener)
    val errorState = MutableLiveData<ErrorState>().apply { value = _errorState }

    private val _isLoading = MutableLiveData<Boolean>().apply { value = true }
    val isLoading: LiveData<Boolean> = _isLoading

    private val _articleList = MutableLiveData<MutableList<BaseRowModel>>().apply {
        value = mutableListOf()
    }

    val articleList: LiveData<MutableList<BaseRowModel>> = _articleList
    val gotList = Transformations.map(articleList) {
        it.size > 0
    }

    fun getArticlesByCategory(category: String, page: Int = 1, isFromSwipeRefresh: Boolean = false) {
        val tempList = mutableListOf<BaseRowModel>()
        errorState.value = _errorState.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val response = newsRepository.getArticlesByCategoryAsync(category)
                withContext(Dispatchers.Main) {
                    response.articles.toMutableList().forEach { article ->
                        article.urlToImage?.let {
                            tempList.add(ArticleRowViewModel(article, this@ArticleViewModel))
                        }
                    }
                    _articleList.value = tempList
                    if (isFromSwipeRefresh) {
                        _event.value = Event(ViewEvent.FinishRefresh)
                    }
                    errorState.value = _errorState.copy(isLoading = false)
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                Log.e(TAG, "exception : ${e.localizedMessage}")
                e.printStackTrace()
                var errorMessage = e.localizedMessage
                withContext(Dispatchers.Main) {
                    if(e.localizedMessage.contains("Unable to resolve host")){
                        errorMessage = "No internet connection"
                    }
                    errorState.value = _errorState.copy(isError = true,errorMessage = errorMessage)
                }
            }
        }
    }

    fun openArticleInBrowser(articleRowViewModel: ArticleRowViewModel) {
        _event.value = Event(ViewEvent.NavigateToBrowser(articleRowViewModel.url))
    }

    override fun onRetry() {
        getArticlesByCategory(category)
    }

    sealed class ViewEvent {
        data class NavigateToBrowser(val url: String) : ViewEvent()
        data class ShowToast(val toastMessage: String) : ViewEvent()
        object FinishRefresh : ViewEvent()
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

}

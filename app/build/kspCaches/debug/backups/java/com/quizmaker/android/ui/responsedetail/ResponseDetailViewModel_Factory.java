package com.quizmaker.android.ui.responsedetail;

import androidx.lifecycle.SavedStateHandle;
import com.quizmaker.android.repository.ResponseDetailRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class ResponseDetailViewModel_Factory implements Factory<ResponseDetailViewModel> {
  private final Provider<ResponseDetailRepository> repositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public ResponseDetailViewModel_Factory(Provider<ResponseDetailRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repositoryProvider = repositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public ResponseDetailViewModel get() {
    return newInstance(repositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static ResponseDetailViewModel_Factory create(
      Provider<ResponseDetailRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new ResponseDetailViewModel_Factory(repositoryProvider, savedStateHandleProvider);
  }

  public static ResponseDetailViewModel newInstance(ResponseDetailRepository repository,
      SavedStateHandle savedStateHandle) {
    return new ResponseDetailViewModel(repository, savedStateHandle);
  }
}

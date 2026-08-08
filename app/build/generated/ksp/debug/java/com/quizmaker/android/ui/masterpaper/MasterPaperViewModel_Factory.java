package com.quizmaker.android.ui.masterpaper;

import androidx.lifecycle.SavedStateHandle;
import com.quizmaker.android.repository.QuizRepository;
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
public final class MasterPaperViewModel_Factory implements Factory<MasterPaperViewModel> {
  private final Provider<QuizRepository> quizRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public MasterPaperViewModel_Factory(Provider<QuizRepository> quizRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.quizRepositoryProvider = quizRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public MasterPaperViewModel get() {
    return newInstance(quizRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static MasterPaperViewModel_Factory create(Provider<QuizRepository> quizRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new MasterPaperViewModel_Factory(quizRepositoryProvider, savedStateHandleProvider);
  }

  public static MasterPaperViewModel newInstance(QuizRepository quizRepository,
      SavedStateHandle savedStateHandle) {
    return new MasterPaperViewModel(quizRepository, savedStateHandle);
  }
}

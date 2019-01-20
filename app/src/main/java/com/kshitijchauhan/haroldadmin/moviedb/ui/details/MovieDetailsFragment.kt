package com.kshitijchauhan.haroldadmin.moviedb.ui.details

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.kshitijchauhan.haroldadmin.moviedb.R
import com.kshitijchauhan.haroldadmin.moviedb.remote.service.account.AddMediaToWatchlistRequest
import com.kshitijchauhan.haroldadmin.moviedb.remote.service.account.MarkMediaAsFavoriteRequest
import com.kshitijchauhan.haroldadmin.moviedb.remote.service.account.MediaTypes
import com.kshitijchauhan.haroldadmin.moviedb.remote.service.movie.Movie
import com.kshitijchauhan.haroldadmin.moviedb.ui.BaseFragment
import com.kshitijchauhan.haroldadmin.moviedb.ui.UIState
import com.kshitijchauhan.haroldadmin.moviedb.ui.common.model.LoadingTask
import com.kshitijchauhan.haroldadmin.moviedb.ui.main.MainViewModel
import com.kshitijchauhan.haroldadmin.moviedb.utils.Constants
import com.kshitijchauhan.haroldadmin.moviedb.utils.extensions.snackbar
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.AbstractYouTubePlayerListener
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_movie_details.*
import kotlinx.android.synthetic.main.fragment_movie_details.view.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class MovieDetailsFragment : BaseFragment() {

    private val TASK_LOAD_MOVIE_DETAILS = "load-movie-details"
    private val TASK_LOAD_MOVIE_ACCOUNT_STATES = "load-account-states"
    private val TASK_TOGGLE_FAVOURITE = "mark-as-favourite"
    private val TASK_TOGGLE_WATCHLIST = "add-to-watchlist"

    private val compositeDisposable = CompositeDisposable()

    private lateinit var movieDetailsViewModel: MovieDetailsViewModel
    private val mainViewModel: MainViewModel by sharedViewModel()

    override val associatedUIState: UIState =
        UIState.DetailsScreenState(this.arguments?.getInt(Constants.KEY_MOVIE_ID) ?: -1)

    override fun notifyBottomNavManager() {
        mainViewModel.updateBottomNavManagerState(this.associatedUIState)
    }

    companion object {
        fun newInstance(movieId: Int, transitionName: String): MovieDetailsFragment {
            val newInstance = MovieDetailsFragment()
            newInstance.arguments = Bundle()
                .apply {
                    putInt(Constants.KEY_MOVIE_ID, movieId)
                    putString(Constants.KEY_TRANSITION_NAME, transitionName)
                }
            return newInstance
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_movie_details, container, false)
        ViewCompat.setTransitionName(view.ivPoster, arguments?.getString(Constants.KEY_TRANSITION_NAME))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(ypvTrailer)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        movieDetailsViewModel = ViewModelProviders.of(this).get(MovieDetailsViewModel::class.java)

        arguments?.getInt(Constants.KEY_MOVIE_ID)?.let { id ->
            with(movieDetailsViewModel) {
                mainViewModel.addLoadingTask(LoadingTask(TASK_LOAD_MOVIE_DETAILS, viewLifecycleOwner))
                getMovieDetails(id)
                if (mainViewModel.isAuthenticated) {
                    mainViewModel.addLoadingTask(LoadingTask(TASK_LOAD_MOVIE_ACCOUNT_STATES, viewLifecycleOwner))
                    getAccountStatesForMovie(id)
                }
            }
        }

        movieDetailsViewModel.movieDetails.observe(viewLifecycleOwner, Observer { movie ->
            mainViewModel.completeLoadingTask(TASK_LOAD_MOVIE_DETAILS, viewLifecycleOwner)
            updateView(movie)
        })

        movieDetailsViewModel.accountStatesOfMovie.observe(viewLifecycleOwner, Observer { state ->
            if (state.favourited) {
                btToggleFavourite.apply {
                    setRemoveFromListState(true)
                    text = "Un-favourite"
                }
            } else {
                btToggleFavourite.apply {
                    setRemoveFromListState(false)
                    text = "Add to Favourites"
                }
            }

            if (state.watchlisted) {
                btToggleWatchlist.apply {
                    setRemoveFromListState(true)
                    text = "Un-watchlist"
                }
            } else {
                btToggleWatchlist.apply {
                    setRemoveFromListState(false)
                    text = "Add to Watchlist"
                }
            }

            mainViewModel.completeLoadingTask(TASK_LOAD_MOVIE_ACCOUNT_STATES, viewLifecycleOwner)

            /*
             We don't know if this update of account states for current movie was triggered by the initial call to
             load movie state, or by toggling favourite/watchlist status of it.
              */
            // TODO Fix this
            mainViewModel.apply {
                completeLoadingTask(TASK_TOGGLE_FAVOURITE, viewLifecycleOwner)
                completeLoadingTask(TASK_TOGGLE_WATCHLIST, viewLifecycleOwner)
            }
        })

        movieDetailsViewModel.trailerUrl.observe(viewLifecycleOwner, Observer { url ->
            ypvTrailer.initialize({ initializedPlayer ->
                initializedPlayer.addListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady() {
                        super.onReady()
                        initializedPlayer.cueVideo(url, 0f)
                    }
                })
            }, true)

        })
    }

    private fun updateView(movie: Movie) {

        mainViewModel.updateToolbarTitle(movie.title)

        Glide.with(this)
            .load(movie.posterPath)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    startPostponedEnterTransition()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    startPostponedEnterTransition()
                    return false
                }

            })
            .into(ivPoster)

        Glide.with(this)
            .asBitmap()
            .transition(BitmapTransitionOptions.withCrossFade())
            .load(movie.backdropPath)
            .into(ivBackdrop)

        tvTitle.text = movie.title
        chipMovieYear.text = movie.releaseDate
        chipMovieGenre.text = movie.genres[0].name
        chipMovieRating.text = String.format("%.2f", movie.voteAverage)
        tvDescription.text = movie.overview
        if (mainViewModel.isAuthenticated) {
            btToggleFavourite.setOnClickListener {
                val isFavourite = movieDetailsViewModel.accountStatesOfMovie.value?.favourited == true
                val request = MarkMediaAsFavoriteRequest(
                    MediaTypes.MOVIE.mediaName,
                    movie.id,
                    !isFavourite
                )
                mainViewModel.addLoadingTask(LoadingTask(TASK_TOGGLE_FAVOURITE, viewLifecycleOwner))
                movieDetailsViewModel.toggleMovieFavouriteStatus(mainViewModel.accountId, request)
            }
            btToggleWatchlist.setOnClickListener {
                val isWatchlisted = movieDetailsViewModel.accountStatesOfMovie.value?.watchlisted == true
                val request = AddMediaToWatchlistRequest(
                    MediaTypes.MOVIE.mediaName,
                    movie.id,
                    !isWatchlisted
                )
                mainViewModel.addLoadingTask(LoadingTask(TASK_TOGGLE_WATCHLIST, viewLifecycleOwner))
                movieDetailsViewModel.toggleMovieWatchlistStatus(mainViewModel.accountId, request)
            }
        } else {
            btToggleFavourite.apply {
                setUnauthenticatedState(true)
                setOnClickListener {
                    snackbar("You need to login to do that.")
                }
            }
            btToggleWatchlist.apply {
                setUnauthenticatedState(true)
                setOnClickListener {
                    snackbar("You need to login to do that.")
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.dispose()
    }

}

package com.kshitijchauhan.haroldadmin.moviedb.repository.movies

import com.kshitijchauhan.haroldadmin.moviedb.repository.actors.Actor
import com.kshitijchauhan.haroldadmin.moviedb.repository.actors.ActorsDao
import com.kshitijchauhan.haroldadmin.moviedb.utils.extensions.log

class LocalMoviesSource(private val moviesDao: MovieDao,
                        private val actorsDao: ActorsDao) {

    fun getMovieFlowable(id: Int) = moviesDao.getMovieFlowable(id)

    fun getAccountStateForMovieFlowable(movieId: Int) = moviesDao.getAccountStatesForMovieFlowable(movieId)

    fun getCastForMovieFlowable(movieId: Int) = moviesDao.getCastForMovieFlowable(movieId)

    fun getMovie(id: Int) = moviesDao.getMovie(id)

    fun getAccountStatesForMovie(movieId: Int) = moviesDao.getAccountStatesForMovie(movieId)

    fun getCastForMovie(movieId: Int) = moviesDao.getCastForMovie(movieId)

    fun isMovieInDatabase(id: Int) = moviesDao.isMovieInDatabase(id)

    fun isAccountStateInDatabase(movieId: Int) = moviesDao.isAccountStateInDatabase(movieId)

    fun isCastInDatabase(movieId: Int) = moviesDao.isCastInDatabase(movieId)

    fun saveMovieToDatabase(movie: Movie) = moviesDao.saveMovie(movie)

    fun saveAccountStateToDatabase(accountState: AccountState) = moviesDao.saveAccountState(accountState)

    fun saveCastToDatabase(cast: Cast) {
        log("Saving cast to database: $cast")
        moviesDao.saveCast(cast)
        cast.castMembers?.let {
            log("Saving cast actors to database: $it")
            actorsDao.saveAllActors(it)
        }
    }

    fun saveMoviesToDatabase(movies: List<Movie>) = moviesDao.saveAllMovies(movies)

    fun saveAccountStatesToDatabase(accountStates: List<AccountState>) = moviesDao.saveAllAccountStates(accountStates)

    fun saveCastsToDatabase(casts: List<Cast>) {
        moviesDao.saveAllCasts(casts)
        casts.forEach { cast ->
            cast.castMembers?.let {
                actorsDao.saveAllActors(it)
            }
        }
    }

    fun saveActorsToDatabase(actors: List<Actor>) = actorsDao.saveAllActors(actors)

    fun updateMovieInDatabase(movie: Movie) = moviesDao.updateMovie(movie)

    fun updateAccountStatesInDatabase(accountState: AccountState) = moviesDao.updateAccountState(accountState)

    fun updateCastInDatabase(cast: Cast) = moviesDao.updateCast(cast)

}

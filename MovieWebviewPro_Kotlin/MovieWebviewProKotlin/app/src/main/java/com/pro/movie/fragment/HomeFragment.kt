package com.pro.movie.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.pro.movie.MyApplication
import com.pro.movie.R
import com.pro.movie.adapter.BannerMovieAdapter
import com.pro.movie.adapter.MovieAdapter
import com.pro.movie.model.Movie
import com.pro.movie.utils.GlobalFunction
import com.pro.movie.utils.StringUtil
import com.pro.movie.utils.Utils
import me.relex.circleindicator.CircleIndicator3
import java.util.*

class HomeFragment : Fragment() {

    private var mView: View? = null
    private var progressDialog: MaterialDialog? = null
    private var edtSearchName: EditText? = null
    private var imgSearch: ImageView? = null
    private var viewPager2: ViewPager2? = null
    private var circleIndicator: CircleIndicator3? = null
    private var rcvData: RecyclerView? = null
    private var layoutContent: LinearLayout? = null
    private var listMovies: MutableList<Movie>? = null
    private var listMovieBanner: MutableList<Movie>? = null
    private val mHandlerBanner: Handler = Handler(Looper.getMainLooper())
    private val mRunnableBanner: Runnable = Runnable {
        if (listMovieBanner == null || listMovieBanner!!.isEmpty()) {
            return@Runnable
        }
        if (viewPager2?.currentItem == listMovieBanner!!.size - 1) {
            viewPager2?.currentItem = 0
            return@Runnable
        }
        viewPager2?.currentItem = viewPager2?.currentItem!! + 1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_home, container, false)
        initUi()
        initListener()
        getListMovies("")
        return mView
    }

    private fun initUi() {
        progressDialog = activity?.let {
            MaterialDialog.Builder(it)
                .content(R.string.waiting_message)
                .progress(true, 0)
                .build()
        }
        layoutContent = mView?.findViewById(R.id.layout_content)
        edtSearchName = mView?.findViewById(R.id.edt_search_name)
        imgSearch = mView?.findViewById(R.id.img_search)
        rcvData = mView?.findViewById(R.id.rcv_data)
        val gridLayoutManager = GridLayoutManager(activity, 3)
        rcvData?.layoutManager = gridLayoutManager
        viewPager2 = mView?.findViewById(R.id.view_pager_2)
        circleIndicator = mView?.findViewById(R.id.indicator_3)
    }

    private fun initListener() {
        imgSearch?.setOnClickListener { searchMovie() }
        edtSearchName?.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchMovie()
                return@setOnEditorActionListener true
            }
            false
        }
        edtSearchName?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val strKey = s.toString().trim { it <= ' ' }
                if (strKey == "" || strKey.isEmpty()) {
                    if (listMovies != null) listMovies!!.clear()
                    getListMovies("")
                }
            }
        })
    }

    private fun searchMovie() {
        val strKey = edtSearchName?.text.toString().trim { it <= ' ' }
        if (listMovies != null) listMovies!!.clear()
        getListMovies(strKey)
        Utils.hideSoftKeyboard(activity)
    }

    private fun getListMovies(key: String?) {
        if (activity == null) {
            return
        }
        progressDialog?.show()
        MyApplication[activity].getMovieDatabaseReference()?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressDialog?.dismiss()
                layoutContent?.visibility = View.VISIBLE
                listMovies = ArrayList()
                for (dataSnapshot in snapshot.children) {
                    val movie: Movie = dataSnapshot.getValue(Movie::class.java) ?: return
                    if (StringUtil.isEmpty(key)) {
                        listMovies?.add(0, movie)
                    } else {
                        if (Utils.getTextSearch(movie.getTitle())!!.trim()
                                .lowercase(Locale.getDefault())
                                        .contains(Utils.getTextSearch(key)!!.trim()
                                            .lowercase(Locale.getDefault()))) {
                            listMovies?.add(0, movie)
                        }
                    }
                }
                displayListBannerMovies()
                displayListAllMovies()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(activity, getString(R.string.msg_get_date_error), Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayListBannerMovies() {
        val bannerMovieAdapter = BannerMovieAdapter(getListBannerMovies(), object : BannerMovieAdapter.IClickItemListener {
            override fun onClickItem(movie: Movie) {
                GlobalFunction.onClickItemMovie(activity, movie)
            }
        })
        viewPager2?.adapter = bannerMovieAdapter
        circleIndicator?.setViewPager(viewPager2)
        viewPager2?.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                mHandlerBanner.removeCallbacks(mRunnableBanner)
                mHandlerBanner.postDelayed(mRunnableBanner, 3000)
            }
        })
    }

    private fun getListBannerMovies(): MutableList<Movie>? {
        if (listMovieBanner != null) {
            listMovieBanner!!.clear()
        } else {
            listMovieBanner = ArrayList()
        }
        if (listMovies == null || listMovies!!.isEmpty()) {
            return listMovieBanner
        }
        for (movie in listMovies!!) {
            if (movie.isFeatured()) {
                listMovieBanner!!.add(movie)
            }
        }
        return listMovieBanner
    }

    private fun displayListAllMovies() {
        val movieAdapter = MovieAdapter(listMovies, object : MovieAdapter.IClickItemListener {
            override fun onClickItem(movie: Movie) {
                GlobalFunction.onClickItemMovie(activity, movie)
            }

            override fun onClickFavorite(movie: Movie, favorite: Boolean) {
                GlobalFunction.onClickFavoriteMovie(activity, movie, favorite)
            }
        })
        rcvData?.adapter = movieAdapter
    }
}
package org.standardnotes.notes.frag

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.frag_note_list.*
import org.joda.time.format.DateTimeFormat
import org.standardnotes.notes.NoteActivity
import org.standardnotes.notes.R
import org.standardnotes.notes.SApplication
import org.standardnotes.notes.comms.data.Note
import org.standardnotes.notes.comms.data.SyncItems
import org.standardnotes.notes.comms.data.UploadSyncItems
import org.standardnotes.notes.dpToPixels
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class NoteListFragment : Fragment() {

    val adapter: Adapter by lazy { Adapter() }

    val notes = ArrayList<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.frag_note_list, container, false)
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.layoutManager = LinearLayoutManager(activity)
        list.adapter = adapter
//        list.addItemDecoration(object : RecyclerView.ItemDecoration() {
//            internal var eight = 8.dpToPixels()
//            internal var sixteen = 16.dpToPixels()
//
//            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
//                val position = parent.getChildAdapterPosition(view)
//                outRect.set(sixteen, eight, sixteen, if (position == adapter.itemCount - 1) eight else 0)
//            }
//        })
        swipeRefreshLayout.isRefreshing = true
        swipeRefreshLayout.setOnRefreshListener { sync() }
        sync()
    }

    fun sync() {
        val uploadSyncItems = UploadSyncItems()
        uploadSyncItems.syncToken = SApplication.instance!!.noteStore.syncToken
        SApplication.instance!!.comms.api.sync(uploadSyncItems).enqueue(object : Callback<SyncItems> {
            override fun onResponse(call: Call<SyncItems>, response: Response<SyncItems>) {
                notes.clear()
                SApplication.instance!!.noteStore.putNotes(response.body())
                notes.addAll(SApplication.instance!!.noteStore.notesList)
                swipeRefreshLayout.isRefreshing = false
                adapter.notifyDataSetChanged()
            }

            override fun onFailure(call: Call<SyncItems>, t: Throwable) {
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    inner class NoteHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var note: Note? = null
            get
            set(value) {
                field = value
                title.text = note?.title
                date.text = DateTimeFormat.shortDateTime().print(note?.original?.updatedAt)
                var noteText = note?.text ?: ""
                noteText = noteText.substring(0, Math.min(256, noteText.length))
                noteText.replace('\n', ' ')
                text.text = noteText
            }
        private val title: TextView = itemView.findViewById(R.id.title) as TextView
        private val date: TextView = itemView.findViewById(R.id.date) as TextView
        private val text: TextView = itemView.findViewById(R.id.text) as TextView

        init {
            itemView.setOnClickListener {
                val intent: Intent = Intent(activity, NoteActivity::class.java)
                intent.putExtra("note", SApplication.instance!!.gson.toJson(note))
                startActivity(intent)
            }
        }


    }

    inner class Adapter : RecyclerView.Adapter<NoteHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): NoteHolder {
            return NoteHolder(LayoutInflater.from(activity).inflate(R.layout.item_note, parent, false))
        }

        override fun getItemCount(): Int {
            return notes.count()
        }

        override fun onBindViewHolder(holder: NoteHolder, position: Int) {
            val note: Note = notes[position]
            holder.note = note
        }

    }

}

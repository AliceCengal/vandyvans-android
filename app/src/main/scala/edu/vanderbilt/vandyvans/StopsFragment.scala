package edu.vanderbilt.vandyvans

import android.support.v4.app.Fragment

import scala.collection.JavaConversions._

import android.os.Bundle
import android.view.{View, ViewGroup, LayoutInflater}
import android.widget.{ListView, AdapterView}

import edu.vanderbilt.vandyvans.models.{Stop, Stops}

class StopsFragment extends Fragment with AdapterView.OnItemClickListener {

  private def stopList = getView.findViewById(R.id.listView1).asInstanceOf[ListView]

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            saved: Bundle) = {
    inflater.inflate(R.layout.fragment_stop, container, false)
  }

  override def onActivityCreated(saved: Bundle) {
    super.onActivityCreated(saved)
    val shortList = Stops.getShortList :+ Stops.buildSimpleStop(-1, "Other Stops")

    stopList.setAdapter(ArrayAdapterBuilder
        .fromCollection(shortList)
        .withContext(getActivity)
        .withResource(R.layout.simple_text)
        .withStringer(StopToString)
        .build)
    stopList.setOnItemClickListener(this)

  }

  override def onItemClick(adapter: AdapterView[_],
                           view: View,
                           position: Int,
                           id: Long) {
    val selectedStop = adapter.getItemAtPosition(position).asInstanceOf[Stop]
    if (selectedStop.id == -1) {
      stopList.setAdapter(ArrayAdapterBuilder
          .fromCollection(Stops.getAll)
          .withContext(getActivity)
          .withResource(R.layout.simple_text)
          .withStringer(StopToString)
          .build)
      stopList.invalidateViews()
    } else {
      DetailActivity.openForId(selectedStop.id, getActivity)
    }
  }

  private object StopToString extends ArrayAdapterBuilder.ToString[Stop] {
    override def apply(s: Stop) = s.name
  }

}
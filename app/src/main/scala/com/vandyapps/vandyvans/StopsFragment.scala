package com.vandyapps.vandyvans

import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.View
import android.widget.{ListView, AdapterView}

import com.marsupial.eventhub.Helpers
import com.vandyapps.vandyvans.models.Stop

class StopsFragment extends Fragment
                    with AdapterView.OnItemClickListener
                    with Helpers.EasySupportFragment
{
  override def layoutId = R.layout.fragment_stop

  private def stopList = component[ListView](R.id.listView1)

  override def onActivityCreated(saved: Bundle) {
    super.onActivityCreated(saved)
    val shortList = Stop.getShortList :+ Stop(-1, "Other Stops")

    stopList.setAdapter(ArrayAdapterBuilder
        .fromCollection(shortList.toArray)
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
          .fromCollection(Stop.getAll.toArray)
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
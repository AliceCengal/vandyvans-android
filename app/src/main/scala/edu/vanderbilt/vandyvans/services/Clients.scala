package edu.vanderbilt.vandyvans.services

import android.os.Handler

trait Clients {
  def vandyVans: Handler
  def syncromatics: Handler
}
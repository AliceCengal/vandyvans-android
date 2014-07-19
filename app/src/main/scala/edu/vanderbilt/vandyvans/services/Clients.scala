package edu.vanderbilt.vandyvans.services

import java.util.logging.Handler

trait Clients {
  def vandyVans: Handler
  def syncromatics: Handler
}
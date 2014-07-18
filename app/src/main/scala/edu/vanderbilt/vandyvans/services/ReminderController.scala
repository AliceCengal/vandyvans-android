package edu.vanderbilt.vandyvans.services

trait ReminderController {
  def subscribeReminderForStop(stopdId: Int): Unit
  def unsubscribeReminderForStop(stopId: Int): Unit
  def isSubscribedToStop(stopId: Int): Boolean
}
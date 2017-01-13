package de.muenchen.mailmerge.event;

import com.google.common.eventbus.Subscribe;

import de.muenchen.mailmerge.event.handlers.OnInitialize;

public class InitEventListener
{
  @Subscribe
  public void onInitialize(OnInitialize event)
  {
    event.process();
  }
}

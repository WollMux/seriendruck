package de.muenchen.mailmerge.event.handlers;

import com.sun.star.document.XEventListener;

import de.muenchen.mailmerge.document.DocumentManager;

/**
 * Erzeugt ein neues WollMuxEvent, das den übergebenen XEventListener
 * deregistriert.
 */
public class OnRemoveDocumentEventListener extends BasicEvent
{
  private XEventListener listener;

  public OnRemoveDocumentEventListener(XEventListener listener)
  {
    this.listener = listener;
  }

  @Override
  protected void doit()
  {
    DocumentManager.getDocumentManager().removeDocumentEventListener(listener);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
  }
}
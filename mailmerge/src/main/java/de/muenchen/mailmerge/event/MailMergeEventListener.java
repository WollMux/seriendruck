package de.muenchen.mailmerge.event;

import com.google.common.eventbus.Subscribe;

import de.muenchen.mailmerge.event.handlers.OnAddDocumentEventListener;
import de.muenchen.mailmerge.event.handlers.OnCloseTextDocument;
import de.muenchen.mailmerge.event.handlers.OnCollectNonWollMuxFormFieldsViaPrintModel;
import de.muenchen.mailmerge.event.handlers.OnCreateDocument;
import de.muenchen.mailmerge.event.handlers.OnExecutePrintFunction;
import de.muenchen.mailmerge.event.handlers.OnHandleMailMergeNewReturned;
import de.muenchen.mailmerge.event.handlers.OnNotifyDocumentEventListener;
import de.muenchen.mailmerge.event.handlers.OnPrint;
import de.muenchen.mailmerge.event.handlers.OnRegisterDispatchInterceptor;
import de.muenchen.mailmerge.event.handlers.OnRemoveDocumentEventListener;
import de.muenchen.mailmerge.event.handlers.OnSeriendruck;
import de.muenchen.mailmerge.event.handlers.OnSetFormValue;
import de.muenchen.mailmerge.event.handlers.OnSetPrintBlocksPropsViaPrintModel;
import de.muenchen.mailmerge.event.handlers.OnSetVisibleState;
import de.muenchen.mailmerge.event.handlers.OnTextDocumentClosed;
import de.muenchen.mailmerge.event.handlers.OnViewCreated;
import de.muenchen.mailmerge.event.handlers.WollMuxEvent;

/**
 * Handler für {@link WollMuxEvent}s.
 * 
 */
public class MailMergeEventListener
{
  MailMergeEventListener() {}
  
  @Subscribe
  public void onAddDocumentEventListener(OnAddDocumentEventListener event)
  {
    event.process();
  }

  @Subscribe
  public void onCloseTextDocument(OnCloseTextDocument event)
  {
    event.process();
  }
  
  @Subscribe
  public void onCollectNonWollMuxFormFieldsViaPrintModel(OnCollectNonWollMuxFormFieldsViaPrintModel event)
  {
    event.process();
  }

  @Subscribe
  public void onExecutePrintFunction(OnExecutePrintFunction event)
  {
    event.process();
  }

  @Subscribe
  public void onHandleMailMergeNewReturned(OnHandleMailMergeNewReturned event)
  {
    event.process();
  }
  
  @Subscribe
  public void onNotifyDocumentEventListener(OnNotifyDocumentEventListener event)
  {
    event.process();
  }

  @Subscribe
  public void onPrint(OnPrint event)
  {
    event.process();
  }

  @Subscribe
  public void onRegisterDispatchInterceptor(OnRegisterDispatchInterceptor event)
  {
    event.process();
  }

  @Subscribe
  public void onRemoveDocumentEventListener(OnRemoveDocumentEventListener event)
  {
    event.process();
  }

  @Subscribe
  public void onSeriendruck(OnSeriendruck event)
  {
    event.process();
  }

  @Subscribe
  public void onSetFormValue(OnSetFormValue event)
  {
    event.process();
  }
  
  @Subscribe
  public void onSetPrintBlocksPropsViaPrintModel(OnSetPrintBlocksPropsViaPrintModel event)
  {
    event.process();
  }

  @Subscribe
  public void onSetVisibleState(OnSetVisibleState event)
  {
    event.process();
  }  
  
  @Subscribe
  public void onTextDocumentClosed(OnTextDocumentClosed event)
  {
    event.process();
  }
  
  @Subscribe
  public void onCreateDocument(OnCreateDocument event)
  {
    event.process();
  }

  @Subscribe
  public void onViewCreated(OnViewCreated event)
  {
    event.process();
  }
}

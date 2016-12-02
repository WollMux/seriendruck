package de.muenchen.allg.itd51.wollmux.event;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnAddDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCloseTextDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCollectNonWollMuxFormFieldsViaPrintModel;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnExecutePrintFunction;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnHandleMailMergeNewReturned;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnInitialize;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnNotifyDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPrint;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRegisterDispatchInterceptor;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRemoveDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSeriendruck;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetFormValue;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetPrintBlocksPropsViaPrintModel;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetVisibleState;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextDocumentClosed;
import de.muenchen.allg.itd51.wollmux.event.handlers.WollMuxEvent;

/**
 * Handler für {@link WollMuxEvent}s.
 * 
 */
public class WollMuxEventListener
{
  WollMuxEventListener() {}
  
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
  public void onInitialize(OnInitialize event)
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
}

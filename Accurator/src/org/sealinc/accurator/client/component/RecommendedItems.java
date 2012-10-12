package org.sealinc.accurator.client.component;

import java.util.List;
import org.sealinc.accurator.client.Accurator;
import org.sealinc.accurator.client.Utility;
import org.sealinc.accurator.shared.CollectionItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class RecommendedItems extends Composite {

	interface MyUiBinder extends UiBinder<Widget, RecommendedItems> {}

	private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
	
	Accurator accurator;

	@UiField
	Label lblCreator1, lblTitle1, lblCreator2, lblTitle2, lblCreator3, lblTitle3;
	@UiField
	Image img1, img2, img3;
	@UiField
	TextBox tbSearch;
	@UiField
	Button btnSearch;

	Label[] titles;
	Label[] creators;
	Image[] images;
	
	@UiHandler("imgNext")
	void nextClick(ClickEvent e){
		loadRecommendations();
	}
	
	@UiHandler("btnSearch")
	void btnSearchClickHandler(ClickEvent e) {
		String text = tbSearch.getText();
		Utility.assignService.search(text, new AsyncCallback<List<CollectionItem>>() {

			@Override
			public void onSuccess(List<CollectionItem> result) {
				loadItems(result);
			}

			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub

			}
		});
	}

	public void loadItems(List<CollectionItem> items) {
		for (int i = 0; i < titles.length; i++) {
			if (items.size() > i) {
				final CollectionItem ci = items.get(i);
				if (ci.maker != null && ci.maker.size() > 0) creators[i].setText(ci.maker.get(0));
				else creators[i].setText("Onbekend");
				titles[i].setText(ci.title);
				images[i].setUrl(ci.imageURL + "&aria/maxwidth_288");
				images[i].setVisible(true);
				images[i].addClickHandler(new ClickHandler() {
					
					@Override
					public void onClick(ClickEvent event) {
						accurator.annotate(ci.uri);	
						loadRecommendations();
					}
				});
			}
			else {
				creators[i].setText("");
				titles[i].setText("");
				images[i].setVisible(false);
			}
		}
	}

	public void loadRecommendations() {
		//clear the existing items
		for(int i=0;i<titles.length;i++){
			creators[i].setText("");
			titles[i].setText("");
			images[i].setVisible(false);
		}
		
		// Get the recommended items
		Utility.assignService.getNextItemsToAnnotate(3, new AsyncCallback<List<String>>() {
			@Override
			public void onSuccess(List<String> result) {
				Utility.itemService.getItems(result, new AsyncCallback<List<CollectionItem>>() {

					@Override
					public void onSuccess(List<CollectionItem> result) {
						loadItems(result);
					}

					@Override
					public void onFailure(Throwable caught) {

					}
				});
			}

			@Override
			public void onFailure(Throwable caught) {

			}
		});
	}

	public RecommendedItems(Accurator acc) {
		initWidget(uiBinder.createAndBindUi(this));
		accurator = acc;
		titles = new Label[] { lblTitle1, lblTitle2, lblTitle3 };
		creators = new Label[] { lblCreator1, lblCreator2, lblCreator3 };
		images = new Image[] { img1, img2, img3 };
		loadRecommendations();
	}

}

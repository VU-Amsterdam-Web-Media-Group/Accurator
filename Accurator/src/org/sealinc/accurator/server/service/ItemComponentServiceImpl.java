/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *******************************************************************************/
package org.sealinc.accurator.server.service;

import java.util.ArrayList;
import java.util.List;
import org.sealinc.accurator.client.service.ItemComponentService;
import org.sealinc.accurator.server.Utility;
import org.sealinc.accurator.shared.CollectionItem;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ItemComponentServiceImpl extends RemoteServiceServlet implements ItemComponentService {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1841696083584950116L;

	@Override
	public List<CollectionItem> getItems(List<String> resourceURIs) {
		if(resourceURIs==null)
			return null;
		else if (resourceURIs.size()==0)
			return new ArrayList<CollectionItem>();
		else
			return Utility.getObjectsByURI(resourceURIs, CollectionItem.class);
	}
}
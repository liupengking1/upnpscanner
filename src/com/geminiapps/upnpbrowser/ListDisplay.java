package com.geminiapps.upnpbrowser;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;


public class ListDisplay {

	Item item;
	Container container;
	Device device;

	final int TYPE_ITEM = 2;
	final int TYPE_CONTAINER = 1;
	final int TYPE_DEVICE=3;

	public ListDisplay(Item item) {
		this.item = item;
		this.container = null;
		this.device=null;
	}

	public ListDisplay(Container container) {
		this.item = null;
		this.container = container;
		this.device=null;
	}
	public ListDisplay(Device device) {
		this.item = null;
		this.container = null;
		this.device=device;
	}

	public Item getItem() {
		return item;
	}

	// return type of ListDisplay, 1 for container, 2 for item
	public int getType() {
		if(device!=null)
			return TYPE_DEVICE;
		if (container != null)
			return TYPE_CONTAINER;
		if (item != null)
			return TYPE_ITEM;
		
		return 0;
	}

	public Container getContainer() {
		return container;
	}
	public Device getDevice(){
		return device;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ListDisplay that = (ListDisplay) o;
		if (item != null)
			return item.equals(that.item);
		if (container != null)
			return container.equals(that.container);
		if (device != null)
			return device.equals(that.device);
		return false;
	}

	@Override
	public int hashCode() {
		if (item != null)
			return item.hashCode();
		if (container != null)
			return container.hashCode();
		if (device != null)
			return device.hashCode();
		return 0;
	}

	@Override
	public String toString() {
		String name = null;
		if (item != null)
			name = item.getTitle();
		if (container != null)
			name = container.getTitle();
		if (device != null)
			name = device.getDetails().getFriendlyName();
		return name;
	}
}
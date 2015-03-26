/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.util.inv;


import java.util.Iterator;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import appeng.api.config.FuzzyMode;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;


public class AdaptorIInventory extends InventoryAdaptor
{

	private final IInventory i;
	private final boolean wrapperEnabled;

	public AdaptorIInventory( IInventory s )
	{
		this.i = s;
		this.wrapperEnabled = s instanceof IInventoryWrapper;
	}

	boolean canRemoveStackFromSlot( int x, ItemStack is )
	{
		if ( this.wrapperEnabled )
			return ( ( IInventoryWrapper ) this.i ).canRemoveItemFromSlot( x, is );
		return true;
	}

	@Override
	public boolean containsItems()
	{
		int s = this.i.getSizeInventory();
		for ( int x = 0; x < s; x++ )
		{
			if ( this.i.getStackInSlot( x ) != null )
				return true;
		}
		return false;
	}

	@Override
	public ItemStack removeSimilarItems( int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination )
	{
		int s = this.i.getSizeInventory();
		for ( int x = 0; x < s; x++ )
		{
			ItemStack is = this.i.getStackInSlot( x );
			if ( is != null && this.canRemoveStackFromSlot( x, is ) && ( filter == null || Platform.isSameItemFuzzy( is, filter, fuzzyMode ) ) )
			{
				int newAmount = amount;
				if ( newAmount > is.stackSize )
					newAmount = is.stackSize;
				if ( destination != null && !destination.canInsert( is ) )
					newAmount = 0;

				ItemStack rv = null;
				if ( newAmount > 0 )
				{
					rv = is.copy();
					rv.stackSize = newAmount;

					if ( is.stackSize == rv.stackSize )
					{
						this.i.setInventorySlotContents( x, null );
						this.i.markDirty();
					}
					else
					{
						ItemStack po = is.copy();
						po.stackSize -= rv.stackSize;
						this.i.setInventorySlotContents( x, po );
						this.i.markDirty();
					}
				}

				if ( rv != null )
				{
					// i.markDirty();
					return rv;
				}
			}
		}
		return null;
	}

	@Override
	public ItemStack simulateSimilarRemove( int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination )
	{
		int s = this.i.getSizeInventory();
		for ( int x = 0; x < s; x++ )
		{
			ItemStack is = this.i.getStackInSlot( x );

			if ( is != null && this.canRemoveStackFromSlot( x, is ) && ( filter == null || Platform.isSameItemFuzzy( is, filter, fuzzyMode ) ) )
			{
				int boundAmount = amount;
				if ( boundAmount > is.stackSize )
					boundAmount = is.stackSize;
				if ( destination != null && !destination.canInsert( is ) )
					boundAmount = 0;

				if ( boundAmount > 0 )
				{
					ItemStack rv = is.copy();
					rv.stackSize = boundAmount;
					return rv;
				}
			}
		}
		return null;
	}

	@Override
	public ItemStack removeItems( int amount, ItemStack filter, IInventoryDestination destination )
	{
		int s = this.i.getSizeInventory();
		ItemStack rv = null;

		for ( int x = 0; x < s && amount > 0; x++ )
		{
			ItemStack is = this.i.getStackInSlot( x );
			if ( is != null && this.canRemoveStackFromSlot( x, is ) && ( filter == null || Platform.isSameItemPrecise( is, filter ) ) )
			{
				int boundAmounts = amount;
				if ( boundAmounts > is.stackSize )
					boundAmounts = is.stackSize;
				if ( destination != null && !destination.canInsert( is ) )
					boundAmounts = 0;

				if ( boundAmounts > 0 )
				{
					if ( rv == null )
					{
						rv = is.copy();
						filter = rv;
						rv.stackSize = boundAmounts;
						amount -= boundAmounts;
					}
					else
					{
						rv.stackSize += boundAmounts;
						amount -= boundAmounts;
					}

					if ( is.stackSize == boundAmounts )
					{
						this.i.setInventorySlotContents( x, null );
						this.i.markDirty();
					}
					else
					{
						ItemStack po = is.copy();
						po.stackSize -= boundAmounts;
						this.i.setInventorySlotContents( x, po );
						this.i.markDirty();
					}
				}
			}
		}

		// if ( rv != null )
		// i.markDirty();

		return rv;
	}

	@Override
	public ItemStack simulateRemove( int amount, ItemStack filter, IInventoryDestination destination )
	{
		int s = this.i.getSizeInventory();
		ItemStack rv = null;

		for ( int x = 0; x < s && amount > 0; x++ )
		{
			ItemStack is = this.i.getStackInSlot( x );
			if ( is != null && this.canRemoveStackFromSlot( x, is ) && ( filter == null || Platform.isSameItemPrecise( is, filter ) ) )
			{
				int boundAmount = amount;
				if ( boundAmount > is.stackSize )
					boundAmount = is.stackSize;
				if ( destination != null && !destination.canInsert( is ) )
					boundAmount = 0;

				if ( boundAmount > 0 )
				{
					if ( rv == null )
					{
						rv = is.copy();
						rv.stackSize = boundAmount;
						amount -= boundAmount;
					}
					else
					{
						rv.stackSize += boundAmount;
						amount -= boundAmount;
					}
				}
			}
		}

		return rv;
	}

	@Override
	public ItemStack addItems( ItemStack toBeAdded )
	{
		return this.addItems( toBeAdded, true );
	}

	@Override
	public ItemStack simulateAdd( ItemStack toBeSimulated )
	{
		return this.addItems( toBeSimulated, false );
	}

	/**
	 * Adds an {@link ItemStack} to the adapted {@link IInventory}.
	 *
	 * It respects the inventories stack limit, which can result in not all items added and some left ones are returned.
	 * The ItemStack next is required for inventories, which will fail on isItemValidForSlot() for stacksizes larger
	 * than the limit.
	 *
	 * @param itemsToAdd itemStack to add to the inventory
	 * @param modulate true to modulate, false for simulate
	 *
	 * @return the left itemstack, which could not be added
	 */
	private ItemStack addItems( ItemStack itemsToAdd, boolean modulate )
	{
		if ( itemsToAdd == null || itemsToAdd.stackSize == 0 )
		{
			return null;
		}

		ItemStack left = itemsToAdd.copy();
		int stackLimit = itemsToAdd.getMaxStackSize();
		int perOperationLimit = Math.min( this.i.getInventoryStackLimit(), stackLimit );
		int inventorySize = this.i.getSizeInventory();

		for ( int slot = 0; slot < inventorySize; slot++ )
		{
			ItemStack next = left.copy();
			next.stackSize = Math.min( perOperationLimit, next.stackSize );

			if ( this.i.isItemValidForSlot( slot, next ) )
			{
				ItemStack is = this.i.getStackInSlot( slot );
				if ( is == null )
				{
					left.stackSize -= next.stackSize;

					if ( modulate )
					{
						this.i.setInventorySlotContents( slot, next );
						this.i.markDirty();
					}

					if ( left.stackSize <= 0 )
					{
						return null;
					}
				}
				else if ( Platform.isSameItemPrecise( is, left ) && is.stackSize < perOperationLimit )
				{
					int room = perOperationLimit - is.stackSize;
					int used = Math.min( left.stackSize, room );

					if ( modulate )
					{
						is.stackSize += used;
						this.i.setInventorySlotContents( slot, is );
						this.i.markDirty();
					}

					left.stackSize -= used;
					if ( left.stackSize <= 0 )
					{
						return null;
					}
				}
			}
		}

		return left;
	}

	class InvIterator implements Iterator<ItemSlot>
	{

		final ItemSlot is = new ItemSlot();
		int x = 0;

		@Override
		public boolean hasNext()
		{
			return this.x < AdaptorIInventory.this.i.getSizeInventory();
		}

		@Override
		public ItemSlot next()
		{
			ItemStack iss = AdaptorIInventory.this.i.getStackInSlot( this.x );

			this.is.isExtractable = AdaptorIInventory.this.canRemoveStackFromSlot( this.x, iss );
			this.is.setItemStack( iss );

			this.is.slot = this.x;
			this.x++;
			return this.is;
		}

		@Override
		public void remove()
		{
			// nothing!
		}

	}

	@Override
	public Iterator<ItemSlot> iterator()
	{
		return new InvIterator();
	}

}

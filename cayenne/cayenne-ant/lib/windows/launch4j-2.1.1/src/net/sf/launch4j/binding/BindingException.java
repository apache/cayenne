/*
 	launch4j :: Cross-platform Java application wrapper for creating Windows native executables
 	Copyright (C) 2005 Grzegorz Kowal

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program; if not, write to the Free Software
	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

/*
 * Created on Apr 30, 2005
 */
package net.sf.launch4j.binding;

/**
 * Signals a runtime error, a missing property in a Java Bean for example.
 * 
 * @author Copyright (C) 2005 Grzegorz Kowal
 */
public class BindingException extends RuntimeException {
	public BindingException(Throwable t) {
		super(t);
	}
	
	public BindingException(String msg) {
		super(msg);
	}
}

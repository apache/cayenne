/*
 * Created on Sep 3, 2005
 */
package net.sf.launch4j.binding;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextArea;
import javax.swing.JToggleButton;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * @author Copyright (C) 2005 Grzegorz Kowal
 */
public class OptListBinding implements Binding, ActionListener {
	private final String _property;
	private final String _stateProperty;
	private final JToggleButton _button;
	private final JTextArea _textArea;
	private final Color _validColor;

	public OptListBinding(String property, String stateProperty, 
			JToggleButton button, JTextArea textArea) {
		if (property == null || button == null || textArea == null) {
			throw new NullPointerException();
		}
		if (property.equals("")) {
			throw new IllegalArgumentException();
		}
		_property = property;
		_stateProperty = stateProperty;
		_button = button;
		_textArea = textArea;
		_validColor = _textArea.getBackground();
		button.addActionListener(this);
	}

	public String getProperty() {
		return _property;
	}

	public void clear(IValidatable bean) {
		put(bean);
	}

	public void put(IValidatable bean) {
		try {
			boolean selected = "true".equals(BeanUtils.getProperty(bean, _stateProperty));
			_button.setSelected(selected);
			_textArea.setEnabled(selected);
			String[] items = (String[]) PropertyUtils.getProperty(bean, _property);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < items.length; i++) {
				sb.append(items[i]);
				if (i < items.length - 1) {
					sb.append("\n");
				}
			}
			_textArea.setText(sb.toString());
		} catch (Exception e) {
			throw new BindingException(e);
		}
	}

	public void get(IValidatable bean) {
		try {
			String[] items = _textArea.getText().split("\n");
			PropertyUtils.setProperty(bean, _property, _button.isSelected() ? items : null);
		} catch (Exception e) {
			throw new BindingException(e);
		}
	}

	public void markValid() {
		_textArea.setBackground(_validColor);
		_textArea.requestFocusInWindow();
	}

	public void markInvalid() {
		_textArea.setBackground(Binding.INVALID_COLOR);
	}
	
	public void setEnabled(boolean enabled) {
		_textArea.setEnabled(enabled);
	}

	public void actionPerformed(ActionEvent e) {
		_textArea.setEnabled(_button.isSelected());
	}
}

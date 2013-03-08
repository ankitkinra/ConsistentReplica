package org.umn.distributed.consistent.common;

public class Parameter {
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[Parameter:");
		builder.append(parameterName);
		builder.append(", type=");
		builder.append(type);
		builder.append(", defaultValue=");
		builder.append(defaultValue);
		builder.append(", isNumber=");
		builder.append(isNumber);
		builder.append("]");
		return builder.toString();
	}

	public static enum PARA_TYPE {
		REQUIRED, OPTIONAL
	};

	private String parameterName;
	private PARA_TYPE type;
	/**
	 * In case of optional parameter you need to specify an defaultValue
	 */
	private String defaultValue;
	private boolean isNumber = false;

	public Parameter(String parameterName) {
		this(parameterName, PARA_TYPE.REQUIRED, null, false);
	}

	public Parameter(String parameterName, boolean isNumber) {
		this(parameterName, PARA_TYPE.REQUIRED, null, isNumber);
	}

	public Parameter(String parameterName, PARA_TYPE type, String defaultValue,
			boolean isNumber) {
		super();
		this.parameterName = parameterName;
		this.type = type;
		if (type == PARA_TYPE.OPTIONAL) {
			if (!Utils.isEmpty(defaultValue)) {
				this.defaultValue = defaultValue;
			} else {
				throw new IllegalArgumentException(
						"optional parameter needs a default value");
			}
		}

		this.isNumber = isNumber;
	}

	public String getParameterName() {
		return parameterName;
	}

	public PARA_TYPE getType() {
		return type;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public boolean isNumber() {
		return isNumber;
	}

	public String validate(String value) throws IllegalArgumentException {
		/**
		 * if required, then cannot be empty, else if optional, then return
		 * value
		 */

		if (this.type == PARA_TYPE.REQUIRED) {
			if (Utils.isEmpty(value)) {
				throw new IllegalArgumentException(this.parameterName
						+ "required parameter came as null or empty");
			}
			if (this.isNumber) {
				if (!Utils.isNumber(value)) {
					throw new IllegalArgumentException(this.parameterName
							+ "required parameter came as null or empty");
				}
			}
			return value.trim();

		} else {
			if (Utils.isEmpty(value)) {
				return this.defaultValue;
			} else {
				return value.trim();
			}
		}
	}

	public boolean isRequired() {
		return this.getType()==PARA_TYPE.REQUIRED;
	}

}

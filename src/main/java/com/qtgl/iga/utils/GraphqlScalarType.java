package com.qtgl.iga.utils;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class GraphqlScalarType {


    public static Logger logger = LoggerFactory.getLogger(GraphqlScalarType.class);


    private static final BigInteger LONG_MAX = BigInteger.valueOf(9223372036854775807L);
    private static final BigInteger LONG_MIN = BigInteger.valueOf(-9223372036854775808L);
    private static final BigInteger INT_MAX = BigInteger.valueOf(2147483647L);
    private static final BigInteger INT_MIN = BigInteger.valueOf(-2147483648L);
    private static final BigInteger BYTE_MAX = BigInteger.valueOf(127L);
    private static final BigInteger BYTE_MIN = BigInteger.valueOf(-128L);
    private static final BigInteger SHORT_MAX = BigInteger.valueOf(32767L);
    private static final BigInteger SHORT_MIN = BigInteger.valueOf(-32768L);


    private static String typeName(Object input) {
        if (input == null) {
            return "null";
        }

        return input.getClass().getSimpleName();
    }

    private static boolean isNumberIsh(Object input) {
        return input instanceof Number || input instanceof String;
    }


    public static final GraphQLScalarType GraphQLUnixTime = new GraphQLScalarType("UnixTime", "timestamp to second", new Coercing<Date, Long>() {
        private Long convertImpl(Object input) {
            if (input instanceof Long) {
                return (Long) input;
            } else if (input instanceof String) {
                Date date = null;
                date = DateUtil.parseDate(input.toString());
                if (null == date) {
                    date = DateUtil.parseShortDate(input.toString());
                }
                return null != date ? date.getTime() : null;
            } else if (input instanceof Timestamp) {
                return ((Timestamp) input).getTime();
            } else if (input instanceof Date) {
                return ((Date) input).getTime();
            } else if (isNumberIsh(input)) {
                BigDecimal value;
                try {
                    value = new BigDecimal(input.toString());
                } catch (NumberFormatException var5) {
                    return null;
                }

                try {
                    return value.longValueExact();
                } catch (ArithmeticException var4) {
                    return null;
                }
            } else if (input instanceof LocalDateTime) {
                return ((LocalDateTime) input).toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
            } else {
                return null;
            }
        }

        @Override
        public Long serialize(Object input) {
            Long result = this.convertImpl(input);
            if (result == null) {
                throw new CoercingSerializeException("Expected type 'UnixTime' but was '" + typeName(input) + "'.");
            } else {
                if (result.toString().length() == 13) {
                    return result / 1000;
                } else if (result.toString().length() == 10) {
                    return result;
                }
                throw new CoercingSerializeException("Expected type 'UnixTime' but was '" + typeName(input) + "'.");

            }
        }

        @Override
        public Date parseValue(Object input) {
            Long result = this.convertImpl(input);
            if (result == null) {
                throw new CoercingParseValueException("Expected type 'UnixTime' but was '" + typeName(input) + "'.");
            } else {
                if (result.toString().length() == 13) {
                    return new Date(result / 1000);
                } else if (result.toString().length() == 10) {
                    return new Date(result);
                }
                throw new CoercingSerializeException("Expected type 'UnixTime' but was '" + typeName(input) + "'.");
            }
        }

        @Override
        public Date parseLiteral(Object input) {
           /* if (input instanceof StringValue) {
                try {
                    return Long.parseLong(((StringValue)input).getValue());
                } catch (NumberFormatException var3) {
                    throw new CoercingParseLiteralException("Expected value to be a UnixTime but it was '" + String.valueOf(input) + "'");
                }
            } else*/
            if (input instanceof IntValue) {
                BigInteger value = ((IntValue) input).getValue();
                if (value.toString().length() == 10) {
                    return new Date(value.longValue() * 1000);
                } else {
                    throw new CoercingParseLiteralException(" UnixTime is timestamp to second ,but is '" + value + "'");
                }
            } else {
                throw new CoercingParseLiteralException("Expected AST type 'IntValue'  but was '" + typeName(input) + "'.");
            }
        }
    });


    public static final GraphQLScalarType GraphQLTimestamp = new GraphQLScalarType("Timestamp", "timestamp to millisecond", new Coercing<Date, Long>() {
        private Long convertImpl(Object input) {
            //logger.info("timestamp convert" + input.getClass().getName());
            if (input instanceof Long) {
                return (Long) input;
            } else if (input instanceof String) {
                Date date = null;
                date = DateUtil.parseDate(input.toString());
                if (null == date) {
                    date = DateUtil.parseShortDate(input.toString());
                }
                return null != date ? date.getTime() : null;
            } else if (input instanceof Timestamp) {
                return ((Timestamp) input).getTime();
            } else if (input instanceof Date) {
                return ((Date) input).getTime();
            } else if (isNumberIsh(input)) {
                BigDecimal value;
                try {
                    value = new BigDecimal(input.toString());
                } catch (NumberFormatException var5) {
                    return null;
                }

                try {
                    return value.longValueExact();
                } catch (ArithmeticException var4) {
                    return null;
                }
            } else if (input instanceof LocalDateTime) {
                return ((LocalDateTime) input).toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
            } else {
                return null;
            }
        }

        @Override
        public Long serialize(Object input) {
            Long result = this.convertImpl(input);
         //   logger.info("timestamp result" + result);

            if (result == null) {
                throw new CoercingSerializeException("Expected type 'Timestamp' but was '" + typeName(input) + "'.");
            } else {
                return result;
                /*if (result.toString().length() == 13) {
                    return result;
                } else if (result.toString().length() == 10) {
                    return result * 1000;
                }
                throw new CoercingSerializeException("Expected type 'Timestamp' but was '" + typeName(input) + "'.");*/

            }
        }

        @Override
        public Date parseValue(Object input) {
            Long result = this.convertImpl(input);
         //   logger.info("timestamp parseValue" + result);

            if (result == null) {
                throw new CoercingParseValueException("Expected type 'Timestamp' but was '" + typeName(input) + "'.");
            } else {
                return new Date(result);
               /* if (result.toString().length() == 13) {
                    return new Date(result);
                } else if (result.toString().length() == 10) {
                    return new Date(result * 1000);
                }
                throw new CoercingSerializeException("Expected type 'Timestamp' but was '" + typeName(input) + "'.");*/
            }
        }

        @Override
        public Date parseLiteral(Object input) {
           /* if (input instanceof StringValue) {
                try {
                    return Long.parseLong(((StringValue)input).getValue());
                } catch (NumberFormatException var3) {
                    throw new CoercingParseLiteralException("Expected value to be a UnixTime but it was '" + String.valueOf(input) + "'");
                }
            } else*/
            if (input instanceof IntValue) {
                BigInteger value = ((IntValue) input).getValue();
                if (value.toString().length() == 13) {
                    return new Date(value.longValue());
                } else {
                    throw new CoercingParseLiteralException(" Timestamp is timestamp to millisecond ,but is '" + value + "'");
                }
            } else {
                throw new CoercingParseLiteralException("Expected AST type 'IntValue'  but was '" + typeName(input) + "'.");
            }
        }
    });


    public static final GraphQLScalarType GraphQLAccount = new GraphQLScalarType("Account", "user account", new Coercing<String, String>() {
        @Override
        public String serialize(Object input) {
            return input.toString();
        }

        @Override
        public String parseValue(Object input) {
            return serialize(input);
        }

        @Override
        public String parseLiteral(Object input) {
            if (!(input instanceof StringValue)) {
                throw new CoercingParseLiteralException(
                        "Expected AST type 'IntValue' but was '" + typeName(input) + "'."
                );
            }
            return ((StringValue) input).getValue();
        }
    });


}

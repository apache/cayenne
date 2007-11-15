CREATE PROCEDURE cayenne_tst_out_proc (IN in_param INT, OUT out_param INT) 
	LANGUAGE SQL
BEGIN 
	SET out_param = in_param * 2;
END
CREATE PROCEDURE cayenne_tst_out_proc @in_param INT, @out_param INT output AS 
BEGIN 
	SELECT @out_param = @in_param * 2
END
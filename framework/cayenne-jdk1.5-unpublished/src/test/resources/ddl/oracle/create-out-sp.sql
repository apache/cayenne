CREATE OR REPLACE PROCEDURE cayenne_tst_out_proc 
(in_param NUMBER, out_param OUT NUMBER) AS 
BEGIN 
	out_param := in_param * 2;
END;
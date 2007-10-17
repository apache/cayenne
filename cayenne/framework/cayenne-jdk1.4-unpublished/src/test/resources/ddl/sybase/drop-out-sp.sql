if exists (SELECT * FROM sysobjects WHERE name = 'cayenne_tst_out_proc') 
BEGIN 
	DROP PROCEDURE cayenne_tst_out_proc 
END
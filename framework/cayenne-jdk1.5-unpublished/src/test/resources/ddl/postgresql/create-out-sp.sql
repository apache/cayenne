CREATE OR REPLACE function cayenne_tst_out_proc (IN int4, OUT int4) RETURNS int4
AS '
BEGIN
	$2 := $1 * 2;
END;
' LANGUAGE plpgsql;
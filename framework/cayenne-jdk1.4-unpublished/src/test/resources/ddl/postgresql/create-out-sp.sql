/* Postgres has no OUT parameters .. this procedure had to be changed to return the value. */
CREATE OR REPLACE function cayenne_tst_out_proc (int4) RETURNS int4
AS '
BEGIN
	return $1 * 2;
END;
' LANGUAGE plpgsql;
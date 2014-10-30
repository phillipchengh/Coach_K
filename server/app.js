var dotenv = require('dotenv');
dotenv.load();
var pg = require('pg');
var express = require('express');
var body_parser = require('body-parser');
var app = express();
app.use(body_parser.json());
app.use(body_parser.urlencoded({
  extended: true
}));

var PORT = process.env.PORT || 1337;

var POSTGRES_CLIENT = {
    user: process.env.POSTGRES_USER || 'postgres',
    password: process.env.POSTGRES_PASSWORD || '',
    database: process.env.POSTGRES_DATABASE || 'coachk',
    host: process.env.POSTGRES_HOST || 'localhost',
    port: process.env.POSTGRES_PORT || 5432
};

app.get('/', function(req, res) {
  res.send('what up :]');
});

app.get('/students', function(req, res) {
  var query_text = 'SELECT * FROM STUDENT';

  var postgres_client = new pg.Client(POSTGRES_CLIENT);

  postgres_client.connect();

  var query = postgres_client.query(query_text);

  query.on('row', function(row, result) {
    result.addRow(row);
  });

  query.on('error', function(error) {
    console.log(error);
    res.json({
      status: 'error',
      data: null,
      message: error.toString()
    });
  });

  query.on('end', function(result) {
    res.json(result.rows);
  });

});

app.get('/student', function(req, res) {
  if (!req.query.user_id) {
    res.json({
      status: 'error',
      data: null,
      message: 'missing user id parameter'
    });
    return;
  }

  var query_text = 
  'SELECT * \
  FROM STUDENT \
  WHERE USER_ID = $1';
  var query_params = [req.query.user_id];

  var postgres_client = new pg.Client(POSTGRES_CLIENT);

  postgres_client.connect();

  var query = postgres_client.query(query_text, query_params);

  query.on('row', function(row, result) {
    result.addRow(row);
  });

  query.on('error', function(error) {
    console.log(error);
    res.json({
      status: 'error',
      data: null,
      message: error.toString()
    });
  });

  query.on('end', function(result) {
    res.json(result.rows);
  });

});

app.post('/student', function(req, res) {
  var query_text = 
  'INSERT \
  INTO STUDENT \
  (USER_ID, GENRE, DISTANCE) \
  VALUES ($1, $2, $3)';
  var query_params = [req.body.user_id, req.body.genre, req.body.distance];

  var postgres_client = new pg.Client(POSTGRES_CLIENT);

  postgres_client.connect();

  postgres_client.query(query_text, query_params, function(error, result) {
    if (error) {
      console.log(error);
      res.json({
        status: 'error',
        data: null,
        message: error.toString()
      });
    } else {
      res.json({
        status: 'success',
        data: result,
        message: 'success'
      });
    }
    postgres_client.end();
  });

});

app.post('/run', function(req, res) {

  if (!req.body.user_id) {
    res.json({
      status: 'error',
      data: null,
      message: 'missing user id parameter'
    });
    return;
  }

  var query_text = 
  'INSERT \
  INTO RUN \
  (USER_ID, RUN_TYPE, SOURCE, DESTINATION, DISTANCE) \
  VALUES ($1, $2, $3, $4, $5)';
  var query_params = [req.body.user_id, req.body.run_type, req.body.source, req.body.destination, req.body.distance];

  var postgres_client = new pg.Client(POSTGRES_CLIENT);

  postgres_client.connect();

  postgres_client.query(query_text, query_params, function(error, result) {
    if (error) {
      console.log(error);
      res.json({
        status: 'error',
        data: null,
        message: error.toString()
      });
    } else {
      res.json({
        status: 'success',
        data: result,
        message: 'success'
      });
    }
    postgres_client.end();
  });

});

app.get('/runs', function(req, res) {

  if (!req.query.user_id) {
    res.json({
      status: 'error',
      data: null,
      message: 'missing user id parameter'
    });
    return;
  }

  var query_text = 
  'SELECT * \
  FROM RUN \
  WHERE USER_ID = $1';
  var query_params = [req.query.user_id];

  var postgres_client = new pg.Client(POSTGRES_CLIENT);

  postgres_client.connect();

  var query = postgres_client.query(query_text, query_params);

  query.on('row', function(row, result) {
    result.addRow(row);
  });

  query.on('error', function(error) {
    console.log(error);
    res.json({
      status: 'error',
      data: null,
      message: error.toString()
    });
  });

  query.on('end', function(result) {
    res.json(result.rows);
  });
   
});

app.listen(PORT);


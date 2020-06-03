/**
* Implements: https://github.com/liquidlabsio/fluidity/issues/62
**/

Fluidity.Dataflow.vue = new Vue({
    el: '#dataflow',
    data() {
      return {
            modelNameInput: {
                name: ""
            },
        }
    },
    mounted() {
        console.log("yayaya")
      },
    methods: {
      fetchModels() {
      			return fetch(SERVICE_URL + '/dataflow/model/list?'
      			    + new URLSearchParams({ tenant: DEFAULT_TENANT })
      			 )
      			.then(res => res.json())
      			 .then(res => {
      			    console.log("Got:" + res);
      			    return res;
      			 });
      		},

    }
})
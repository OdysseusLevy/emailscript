
<h2>Counts for {{folder}}</h2>
Total emails: {{size}}<p/>
<table cellpadding="0" cellspacing="3">
      <tr>
         <th>Count</th>
         <th>From</th>
         <th>Size</th>
         <th>% Read</th>
      </tr>
      {{#counts}}
      <tr style="background-color:{{backgroundColor}}">
         <td>{{count}}</td>
         <td>{{from}}</td>
         <td>{{sizeInBytes}}</td>
         <td>{{percentRead}}</td>
      </tr>
      {{/counts}}
</table>
